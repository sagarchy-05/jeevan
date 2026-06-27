package com.jeevan.core.event;

import java.time.Instant;

/**
 * Wire payload for {@code appointment.cancelled} (core → worker). Same shape as
 * {@link AppointmentBookedEvent}; {@code eventType} distinguishes them.
 */
public record AppointmentCancelledEvent(
        String eventId,
        String eventType,
        Long appointmentId,
        Long patientId,
        String patientEmail,
        String patientName,
        Long doctorId,
        String doctorName,
        String specialty,
        Instant startTime,
        Instant endTime,
        Instant occurredAt
) {
}
