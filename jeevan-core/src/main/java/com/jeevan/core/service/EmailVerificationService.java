package com.jeevan.core.service;

import com.jeevan.core.event.VerificationRequestedEvent;
import com.jeevan.core.exception.VerificationTokenInvalidException;
import com.jeevan.core.model.EmailVerificationToken;
import com.jeevan.core.model.User;
import com.jeevan.core.repository.EmailVerificationTokenRepository;
import com.jeevan.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Email-verification feature (§5a), gated by {@code jeevan.email.verification-enabled}.
 * Issues opaque single-use tokens, verifies them, and resends. The
 * {@code verification.requested} event is published after commit (via the shared
 * {@link org.springframework.context.ApplicationEventPublisher}) so the worker only
 * emails for users that are actually persisted.
 */
@Service
public class EmailVerificationService {

    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final ApplicationEventPublisher events;
    private final boolean enabled;
    private final long ttlHours;
    private final String frontendBaseUrl;

    public EmailVerificationService(
            UserRepository users,
            EmailVerificationTokenRepository tokens,
            ApplicationEventPublisher events,
            @Value("${jeevan.email.verification-enabled:false}") boolean enabled,
            @Value("${jeevan.email.verification-ttl-hours:24}") long ttlHours,
            @Value("${jeevan.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.users = users;
        this.tokens = tokens;
        this.events = events;
        this.enabled = enabled;
        this.ttlHours = ttlHours;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Invalidates any active token, issues a fresh one, and emits the verification
     * event. Must run inside the caller's transaction (register / resend).
     */
    public void issueAndPublish(User user) {
        Instant now = Instant.now();
        tokens.invalidateActiveTokens(user.getId(), now);

        String token = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(ttlHours, ChronoUnit.HOURS);
        tokens.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(expiresAt)
                .build());

        String verificationLink = frontendBaseUrl + "/verify?token=" + token;
        events.publishEvent(new VerificationRequestedEvent(
                UUID.randomUUID().toString(), "VERIFICATION_REQUESTED",
                user.getId(), user.getEmail(), user.getFullName(),
                verificationLink, expiresAt, now));
    }

    @Transactional
    public void verify(String token) {
        EmailVerificationToken record = tokens.findByToken(token)
                .orElseThrow(VerificationTokenInvalidException::new);

        Instant now = Instant.now();
        if (record.getConsumedAt() != null || record.getExpiresAt().isBefore(now)) {
            throw new VerificationTokenInvalidException();
        }

        record.setConsumedAt(now);
        tokens.save(record);

        User user = users.findById(record.getUserId())
                .orElseThrow(VerificationTokenInvalidException::new);
        user.setEmailVerified(true);
        users.save(user);
    }

    /** @return true if a fresh verification email was sent; false if already verified (no-op). */
    @Transactional
    public boolean resend(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(VerificationTokenInvalidException::new);
        if (user.isEmailVerified()) {
            return false;
        }
        issueAndPublish(user);
        return true;
    }
}
