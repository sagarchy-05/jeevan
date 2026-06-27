package com.jeevan.core.event;

import java.time.Instant;

/**
 * Wire payload for {@code verification.requested} (core → worker). The worker emails
 * the link; it does not publish a notification.sent back (no appointment to attach to).
 */
public record VerificationRequestedEvent(
        String eventId,
        String eventType,
        Long userId,
        String email,
        String fullName,
        String verificationLink,
        Instant expiresAt,
        Instant occurredAt
) {
}
