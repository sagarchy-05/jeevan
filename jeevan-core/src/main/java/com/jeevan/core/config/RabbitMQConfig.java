package com.jeevan.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the shared topology in code (idempotent): a durable topic exchange, the
 * notifier queue (appointment + verification events) and the core queue (notification
 * results), with their bindings. RabbitAdmin declares these on first connection, so
 * whichever service connects first builds the topology. JSON messages use the
 * Spring-managed ObjectMapper so instants serialize as ISO-8601, camelCase.
 */
@Configuration
public class RabbitMQConfig {

    private final RabbitProperties props;

    public RabbitMQConfig(RabbitProperties props) {
        this.props = props;
    }

    @Bean
    public TopicExchange appointmentsExchange() {
        return ExchangeBuilder.topicExchange(props.exchange()).durable(true).build();
    }

    @Bean
    public Queue notifierQueue() {
        return QueueBuilder.durable(props.notifierQueue()).build();
    }

    @Bean
    public Queue coreQueue() {
        return QueueBuilder.durable(props.coreQueue()).build();
    }

    @Bean
    public Binding notifierBookedBinding() {
        return BindingBuilder.bind(notifierQueue()).to(appointmentsExchange())
                .with(props.routingKeys().appointmentBooked());
    }

    @Bean
    public Binding notifierCancelledBinding() {
        return BindingBuilder.bind(notifierQueue()).to(appointmentsExchange())
                .with(props.routingKeys().appointmentCancelled());
    }

    @Bean
    public Binding notifierVerificationBinding() {
        return BindingBuilder.bind(notifierQueue()).to(appointmentsExchange())
                .with(props.routingKeys().verificationRequested());
    }

    @Bean
    public Binding coreNotificationBinding() {
        return BindingBuilder.bind(coreQueue()).to(appointmentsExchange())
                .with(props.routingKeys().notificationSent());
    }

    /** Picked up by Boot's auto-configured RabbitTemplate and listener factory. */
    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
