package com.jeevan.core.dto.response;

import java.util.List;

/**
 * A doctor's recurring weekly windows. The frontend derives the set of working
 * weekdays from {@code windows} to enable/disable calendar days.
 */
public record AvailabilityResponse(
        Long doctorId,
        String doctorName,
        List<Window> windows
) {
    public record Window(
            String dayOfWeek,
            String startTime,
            String endTime
    ) {
    }
}
