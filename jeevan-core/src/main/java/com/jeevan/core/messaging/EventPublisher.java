package com.jeevan.core.messaging;

import com.jeevan.core.config.RabbitProperties;
import com.jeevan.core.event.AppointmentBookedEvent;
import com.jeevan.core.event.AppointmentCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes appointment events to RabbitMQ, but only {@code AFTER_COMMIT} of the
 * booking/cancellation transaction — so the worker only ever reacts to facts that
 * are already durable. The service emits a Spring application event inside the
 * transaction; these listeners forward it to the broker once it commits.
 *
 * <p>A broker failure here does not fail the (already-committed) request — the
 * appointment simply stays {@code notificationStatus = PENDING}. No outbox/retry
 * (out of scope).
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties props;

    public EventPublisher(RabbitTemplate rabbitTemplate, RabbitProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentBooked(AppointmentBookedEvent event) {
        send(props.routingKeys().appointmentBooked(), event, event.appointmentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        send(props.routingKeys().appointmentCancelled(), event, event.appointmentId());
    }

    private void send(String routingKey, Object payload, Long appointmentId) {
        try {
            rabbitTemplate.convertAndSend(props.exchange(), routingKey, payload);
            log.info("Published '{}' for appointment {}", routingKey, appointmentId);
        } catch (Exception e) {
            log.error("Failed to publish '{}' for appointment {}: {}", routingKey, appointmentId, e.getMessage());
        }
    }
}
