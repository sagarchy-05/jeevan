package com.jeevan.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clinic/booking rules bound from {@code jeevan.*}: the timezone availability is
 * interpreted in, the fixed slot length, and how many days ahead booking is open.
 */
@ConfigurationProperties(prefix = "jeevan")
public record ClinicProperties(
        String clinicTimezone,
        int slotLengthMinutes,
        int bookingWindowDays
) {
}
