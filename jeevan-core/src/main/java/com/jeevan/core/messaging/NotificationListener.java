package com.jeevan.core.messaging;

import com.jeevan.core.event.NotificationSentEvent;
import com.jeevan.core.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code notification.sent} from the core queue and updates the
 * appointment's notification status — the closing half of the round-trip. The
 * JSON body is converted to {@link NotificationSentEvent} via the shared Jackson
 * message converter (inferred from this method's parameter type).
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final AppointmentService appointmentService;

    public NotificationListener(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @RabbitListener(queues = "${jeevan.rabbitmq.core-queue}")
    public void onNotificationSent(NotificationSentEvent event) {
        log.info("notification.sent received for appointment {} -> {}",
                event.appointmentId(), event.status());
        appointmentService.applyNotificationResult(event.appointmentId(), event.status(), event.detail());
    }
}
