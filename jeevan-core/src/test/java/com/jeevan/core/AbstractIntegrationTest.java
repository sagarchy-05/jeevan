package com.jeevan.core;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for integration tests. Boots a real Postgres 16 once (singleton container,
 * shared across all test classes) and points the datasource at it; Flyway then
 * applies the real migrations + seed. No H2 anywhere.
 *
 * <p>No RabbitMQ broker is started for these tests, so the {@code notification.sent}
 * listener is kept from auto-starting; event publishing failures are swallowed by
 * design, so booking/cancellation still work without a broker.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
    }
}
