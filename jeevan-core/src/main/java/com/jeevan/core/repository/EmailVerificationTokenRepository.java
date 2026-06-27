package com.jeevan.core.repository;

import com.jeevan.core.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    /** Marks all of a user's still-active tokens as consumed (used on resend). */
    @Modifying
    @Query("""
            update EmailVerificationToken t set t.consumedAt = :now
            where t.userId = :userId and t.consumedAt is null
            """)
    void invalidateActiveTokens(@Param("userId") Long userId, @Param("now") Instant now);
}
