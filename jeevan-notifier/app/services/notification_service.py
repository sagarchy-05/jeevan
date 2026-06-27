"""Pluggable notification sender.

Logs by default (the fast-path build); when NOTIFIER_EMAIL_ENABLED is on, the
same interface sends through MailHog/SMTP. Raises on a real send failure so the
consumer can report status=FAILED.
"""
import logging
import smtplib
from dataclasses import dataclass
from datetime import datetime
from email.message import EmailMessage
from zoneinfo import ZoneInfo

from app.config import Settings, settings
from app.schemas.events import AppointmentEvent, VerificationRequested

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

    def notify_verification(self, event: VerificationRequested) -> NotificationOutcome:
        subject = "Verify your Jeevan account"
        body = (
            f"Hi {event.full_name},\n\n"
            f"Welcome to Jeevan! Please confirm your email address to start booking "
            f"appointments by opening the link below:\n\n"
            f"    {event.verification_link}\n\n"
            f"This link expires on {self._human_time(event.expires_at)}.\n\n"
            f"If you didn't create a Jeevan account, you can safely ignore this email.\n\n"
            f"— The Jeevan Team"
        )
        if self.config.email_enabled:
            self._send_email(event.email, subject, body)
            return NotificationOutcome("SENT", "EMAIL", f"Verification email sent to {event.email}")
        log.info("[VERIFY] to=%s | %s", event.email, event.verification_link)
        return NotificationOutcome("SENT", "LOG", f"Verification link logged for {event.email}")

    def _compose(self, event: AppointmentEvent) -> tuple[str, str]:
        when = self._human_time(event.start_time)
        if event.event_type == "APPOINTMENT_CANCELLED":
            return (
                f"Your appointment with {event.doctor_name} has been cancelled",
                f"Hi {event.patient_name},\n\n"
                f"Your {event.specialty} appointment with {event.doctor_name} on "
                f"{when} has been cancelled.\n\n"
                f"You can book a new time whenever you're ready from your Jeevan account.\n\n"
                f"— The Jeevan Team",
            )
        return (
            f"Your appointment with {event.doctor_name} is confirmed",
            f"Hi {event.patient_name},\n\n"
            f"Your {event.specialty} appointment with {event.doctor_name} is confirmed for "
            f"{when}.\n\n"
            f"Need to make a change? You can cancel from your Jeevan account any time before "
            f"the appointment starts.\n\n"
            f"— The Jeevan Team",
        )

    def _human_time(self, value: datetime) -> str:
        """Render a UTC instant in the clinic timezone, e.g. 'Thursday, 2 July 2026 at 5:00 PM'."""
        local = value.astimezone(ZoneInfo(self.config.clinic_timezone))
        hour = local.strftime("%I").lstrip("0") or "12"
        return f"{local.strftime('%A')}, {local.day} {local.strftime('%B %Y')} at {hour}:{local.strftime('%M %p')}"

    def _send_email(self, recipient: str, subject: str, body: str) -> None:
        message = EmailMessage()
        message["From"] = self.config.mail_from
        message["To"] = recipient
        message["Subject"] = subject
        message.set_content(body)
        with smtplib.SMTP(self.config.mail_host, self.config.mail_port, timeout=10) as smtp:
            smtp.send_message(message)


notification_service = NotificationService(settings)
