package com.jeevan.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code jeevan.jwt.*}. {@code expiry-minutes} relaxed-binds to expiryMinutes.
 */
@ConfigurationProperties(prefix = "jeevan.jwt")
public record JwtProperties(String secret, long expiryMinutes) {
}
