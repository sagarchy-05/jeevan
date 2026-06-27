"""Blocking pika consumer (runs on a background thread).

Declares its half of the topology idempotently, consumes appointment events,
delegates to the notification service, records the outcome, and publishes
notification.sent back. On failure it reports status=FAILED and discards the
message (no dead-letter / retry — out of scope). Reconnects on connection loss.
"""
import json
import logging
import time
from datetime import datetime, timezone
from uuid import uuid4

import pika

from app.config import settings
from app.messaging.publisher import publish_notification_sent
from app.schemas.events import AppointmentEvent, NotificationSent
from app.services.notification_service import notification_service
from app.store.notification_store import store

log = logging.getLogger(__name__)

APPOINTMENT_EVENTS = {"APPOINTMENT_BOOKED", "APPOINTMENT_CANCELLED"}


class NotificationConsumer:
    def __init__(self):
        self._connection = None
        self._channel = None
        self._should_run = True

    def run(self) -> None:
        while self._should_run:
            try:
                self._connect_and_consume()
            except pika.exceptions.AMQPConnectionError:
                if not self._should_run:
                    break
                log.warning("RabbitMQ unavailable; retrying in 5s")
                time.sleep(5)
            except Exception:
                if not self._should_run:
                    break
                log.exception("Consumer crashed; restarting in 5s")
                time.sleep(5)

    def _connect_and_consume(self) -> None:
        credentials = pika.PlainCredentials(settings.rabbitmq_user, settings.rabbitmq_password)
        params = pika.ConnectionParameters(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            credentials=credentials,
            heartbeat=30,
            blocked_connection_timeout=300,
        )
        self._connection = pika.BlockingConnection(params)
        self._channel = self._connection.channel()

        # Declare our half of the topology (idempotent — matches the core's).
        self._channel.exchange_declare(exchange=settings.exchange, exchange_type="topic", durable=True)
        self._channel.queue_declare(queue=settings.notifier_queue, durable=True)
        for routing_key in (
            settings.rk_appointment_booked,
            settings.rk_appointment_cancelled,
            settings.rk_verification_requested,
        ):
            self._channel.queue_bind(
                queue=settings.notifier_queue, exchange=settings.exchange, routing_key=routing_key
            )

        self._channel.basic_qos(prefetch_count=10)
        self._channel.basic_consume(queue=settings.notifier_queue, on_message_callback=self._on_message)
        log.info("Notifier consuming from '%s'", settings.notifier_queue)
        self._channel.start_consuming()

    def _on_message(self, channel, method, properties, body) -> None:
        try:
            data = json.loads(body)
        except Exception:
            log.exception("Unparseable message; discarding")
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            return

        event_type = data.get("eventType")
        try:
            if event_type in APPOINTMENT_EVENTS:
                self._handle_appointment(channel, data)
            elif event_type == "VERIFICATION_REQUESTED":
                self._handle_verification(data)
            else:
                log.warning("Ignoring unknown eventType: %s", event_type)
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except Exception as exc:
            log.exception("Failed to process %s", event_type)
            self._report_failure(channel, data, str(exc))
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    def _handle_appointment(self, channel, data: dict) -> None:
        event = AppointmentEvent.model_validate(data)
        outcome = notification_service.notify_appointment(event)
        processed_at = datetime.now(timezone.utc)

        store.add({
            "appointmentId": event.appointment_id,
            "eventType": event.event_type,
            "recipient": event.patient_email,
            "status": outcome.status,
            "channel": outcome.channel,
            "detail": outcome.detail,
            "processedAt": processed_at.isoformat(),
        })

        sent = NotificationSent(
            event_id=str(uuid4()),
            appointment_id=event.appointment_id,
            status=outcome.status,
            channel=outcome.channel,
            detail=outcome.detail,
            processed_at=processed_at,
        )
        publish_notification_sent(channel, settings.exchange, settings.rk_notification_sent, sent)
        log.info("Processed %s for appointment %s -> %s",
                 event.event_type, event.appointment_id, outcome.status)

    def _handle_verification(self, data: dict) -> None:
        # Real email send lands with the email-verification feature (step 11);
        # for now just record receipt so the binding is exercised.
        email = data.get("email")
        log.info("Verification requested for %s (email send handled in the verification step)", email)
        store.add({
            "appointmentId": None,
            "eventType": "VERIFICATION_REQUESTED",
            "recipient": email,
            "status": "SENT",
            "channel": "LOG",
            "detail": "Verification event received",
            "processedAt": datetime.now(timezone.utc).isoformat(),
        })

    def _report_failure(self, channel, data: dict, error: str) -> None:
        appointment_id = data.get("appointmentId")
        processed_at = datetime.now(timezone.utc)
        channel_name = "EMAIL" if settings.email_enabled else "LOG"
        store.add({
            "appointmentId": appointment_id,
            "eventType": data.get("eventType"),
            "recipient": data.get("patientEmail"),
            "status": "FAILED",
            "channel": channel_name,
            "detail": f"Processing failed: {error}",
            "processedAt": processed_at.isoformat(),
        })
        if appointment_id is None:
            return
        try:
            sent = NotificationSent(
                event_id=str(uuid4()),
                appointment_id=appointment_id,
                status="FAILED",
                channel=channel_name,
                detail=f"Notification failed: {error}",
                processed_at=processed_at,
            )
            publish_notification_sent(channel, settings.exchange, settings.rk_notification_sent, sent)
        except Exception:
            log.exception("Could not publish FAILED notification for appointment %s", appointment_id)

    def stop(self) -> None:
        self._should_run = False
        try:
            if self._connection and self._connection.is_open:
                self._connection.add_callback_threadsafe(self._safe_close)
        except Exception:
            pass

    def _safe_close(self) -> None:
        try:
            if self._channel and self._channel.is_open:
                self._channel.stop_consuming()
        finally:
            if self._connection and self._connection.is_open:
                self._connection.close()
