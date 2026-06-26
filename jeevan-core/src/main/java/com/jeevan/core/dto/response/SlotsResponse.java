package com.jeevan.core.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Dynamically generated available 30-minute slots for a doctor on a date.
 * {@code start}/{@code end} are UTC instants (what the client books with);
 * {@code label} is the local clinic start time (HH:mm) for display.
 */
public record SlotsResponse(
        Long doctorId,
        LocalDate date,
        List<Slot> slots
) {
    public record Slot(
            Instant start,
            Instant end,
            String label
    ) {
    }
}
