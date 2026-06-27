package com.jeevan.core.event;

import java.time.Instant;

/**
 * Wire payload for {@code notification.sent} (worker → core). {@code status} is
 * {@code SENT} or {@code FAILED}.
 */
public record NotificationSentEvent(
        String eventId,
        Long appointmentId,
        String status,
        String channel,
        String detail,
        Instant processedAt
) {
}
