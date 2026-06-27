"""Publishes notification.sent back to the core.

Reuses the consumer's channel (same thread), so no second connection is needed.
"""
import pika

from app.schemas.events import NotificationSent


def publish_notification_sent(channel, exchange: str, routing_key: str, event: NotificationSent) -> None:
    body = event.model_dump_json(by_alias=True).encode("utf-8")
    channel.basic_publish(
        exchange=exchange,
        routing_key=routing_key,
        body=body,
        properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
    )
