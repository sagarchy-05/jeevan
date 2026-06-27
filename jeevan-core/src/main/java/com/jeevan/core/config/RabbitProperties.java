package com.jeevan.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Broker topology bound from {@code jeevan.rabbitmq.*}. Names must match the worker
 * exactly; defaults live in application.yml / .env.
 */
@ConfigurationProperties(prefix = "jeevan.rabbitmq")
public record RabbitProperties(
        String exchange,
        String notifierQueue,
        String coreQueue,
        RoutingKeys routingKeys
) {
    public record RoutingKeys(
            String appointmentBooked,
            String appointmentCancelled,
            String notificationSent,
            String verificationRequested
    ) {
    }
}
