"""Environment-driven configuration (pydantic-settings).

Field aliases map to the same env var names the core/compose use, so the broker
topology stays in lock-step across services. Unknown env vars are ignored.
"""
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore", populate_by_name=True)

    app_name: str = "jeevan-notifier"

    # RabbitMQ connection
    rabbitmq_host: str = Field(default="localhost", alias="RABBITMQ_HOST")
    rabbitmq_port: int = Field(default=5672, alias="RABBITMQ_PORT")
    rabbitmq_user: str = Field(default="jeevan", alias="RABBITMQ_DEFAULT_USER")
    rabbitmq_password: str = Field(default="jeevan", alias="RABBITMQ_DEFAULT_PASS")

    # Topology (must match the core)
    exchange: str = Field(default="jeevan.appointments", alias="RABBITMQ_EXCHANGE")
    notifier_queue: str = Field(default="notifier.appointment-events", alias="RABBITMQ_NOTIFIER_QUEUE")
    rk_appointment_booked: str = Field(default="appointment.booked", alias="RK_APPOINTMENT_BOOKED")
    rk_appointment_cancelled: str = Field(default="appointment.cancelled", alias="RK_APPOINTMENT_CANCELLED")
    rk_notification_sent: str = Field(default="notification.sent", alias="RK_NOTIFICATION_SENT")
    rk_verification_requested: str = Field(default="verification.requested", alias="RK_VERIFICATION_REQUESTED")

    # Notification channel: log-only by default; MailHog/SMTP when enabled
    email_enabled: bool = Field(default=False, alias="NOTIFIER_EMAIL_ENABLED")
    mail_host: str = Field(default="mailhog", alias="MAIL_HOST")
    mail_port: int = Field(default=1025, alias="MAIL_PORT")
    mail_from: str = Field(default="no-reply@jeevan.local", alias="MAIL_FROM")


settings = Settings()
