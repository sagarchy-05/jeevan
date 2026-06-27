"""Pluggable notification sender.

Logs by default (the fast-path build); when NOTIFIER_EMAIL_ENABLED is on, the
same interface sends through MailHog/SMTP. Raises on a real send failure so the
consumer can report status=FAILED.
"""
import logging
import smtplib
from dataclasses import dataclass
from email.message import EmailMessage

from app.config import Settings, settings
from app.schemas.events import AppointmentEvent

log = logging.getLogger(__name__)


@dataclass
class NotificationOutcome:
    status: str   # SENT | FAILED
    channel: str  # EMAIL | LOG
    detail: str


class NotificationService:
    def __init__(self, config: Settings):
        self.config = config

    def notify_appointment(self, event: AppointmentEvent) -> NotificationOutcome:
        subject, body = self._compose(event)
        recipient = event.patient_email
        if self.config.email_enabled:
            self._send_email(recipient, subject, body)
            return NotificationOutcome("SENT", "EMAIL", f"Email sent to {recipient}")
        log.info("[NOTIFY] to=%s | %s", recipient, subject)
        return NotificationOutcome("SENT", "LOG", f"Notification logged for {recipient}")

    def _compose(self, event: AppointmentEvent) -> tuple[str, str]:
        when = event.start_time.isoformat()
        if event.event_type == "APPOINTMENT_CANCELLED":
            return (
                f"Your appointment with {event.doctor_name} was cancelled",
                f"Hi {event.patient_name}, your {event.specialty} appointment with "
                f"{event.doctor_name} on {when} has been cancelled.",
            )
        return (
            f"Appointment confirmed with {event.doctor_name}",
            f"Hi {event.patient_name}, your {event.specialty} appointment with "
            f"{event.doctor_name} is confirmed for {when}.",
        )

    def _send_email(self, recipient: str, subject: str, body: str) -> None:
        message = EmailMessage()
        message["From"] = self.config.mail_from
        message["To"] = recipient
        message["Subject"] = subject
        message.set_content(body)
        with smtplib.SMTP(self.config.mail_host, self.config.mail_port, timeout=10) as smtp:
            smtp.send_message(message)


notification_service = NotificationService(settings)
