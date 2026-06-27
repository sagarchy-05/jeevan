"""Pydantic models mirroring the wire contracts (camelCase on the wire)."""
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class AppointmentEvent(BaseModel):
    """Inbound appointment.booked / appointment.cancelled payload (core -> worker)."""

    model_config = ConfigDict(populate_by_name=True)

    event_id: str = Field(alias="eventId")
    event_type: str = Field(alias="eventType")
    appointment_id: int = Field(alias="appointmentId")
    patient_id: int = Field(alias="patientId")
    patient_email: str = Field(alias="patientEmail")
    patient_name: str = Field(alias="patientName")
    doctor_id: int = Field(alias="doctorId")
    doctor_name: str = Field(alias="doctorName")
    specialty: str
    start_time: datetime = Field(alias="startTime")
    end_time: datetime = Field(alias="endTime")
    occurred_at: datetime = Field(alias="occurredAt")


class NotificationSent(BaseModel):
    """Outbound notification.sent payload (worker -> core)."""

    model_config = ConfigDict(populate_by_name=True)

    event_id: str = Field(alias="eventId")
    appointment_id: int = Field(alias="appointmentId")
    status: str
    channel: str
    detail: str
    processed_at: datetime = Field(alias="processedAt")


class VerificationRequested(BaseModel):
    """Inbound verification.requested payload (core -> worker)."""

    model_config = ConfigDict(populate_by_name=True)

    event_id: str = Field(alias="eventId")
    event_type: str = Field(alias="eventType")
    user_id: int = Field(alias="userId")
    email: str
    full_name: str = Field(alias="fullName")
    verification_link: str = Field(alias="verificationLink")
    expires_at: datetime = Field(alias="expiresAt")
    occurred_at: datetime = Field(alias="occurredAt")
