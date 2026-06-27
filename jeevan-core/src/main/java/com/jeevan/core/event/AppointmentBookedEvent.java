package com.jeevan.core.event;

import java.time.Instant;

/**
 * Wire payload for {@code appointment.booked} (core → worker). Serialized as
 * camelCase JSON. Plain values only — safe to use after the transaction commits.
 */
public record AppointmentBookedEvent(
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
