package com.jeevan.core.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * {@code startTime} is the UTC instant of the chosen slot (the {@code start} value
 * returned by the slots endpoint).
 */
public record BookAppointmentRequest(
        @NotNull Long doctorId,
        @NotNull Instant startTime
) {
}
