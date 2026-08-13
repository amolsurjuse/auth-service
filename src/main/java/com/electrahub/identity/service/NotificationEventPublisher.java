package com.electrahub.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final String tenantId;

    public NotificationEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.notification.exchange}") String exchange,
            @Value("${app.notification.domain-routing-key}") String routingKey,
            @Value("${app.notification.tenant-id}") String tenantId
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.tenantId = tenantId;
    }

    public void publish(String eventType, UUID userId, String recipientRef, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, new DomainNotificationEvent(
                    UUID.randomUUID().toString(),
                    eventType,
                    tenantId,
                    recipientRef,
                    userId == null ? null : userId.toString(),
                    OffsetDateTime.now().toString(),
                    payload == null ? Map.of() : payload
            ));
        } catch (RuntimeException ex) {
            log.warn("Notification event publish failed and will be ignored eventType={} userId={}: {}", eventType, userId, ex.getMessage());
        }
    }

    public record DomainNotificationEvent(
            String eventId,
            String eventType,
            String tenantId,
            String recipientRef,
            String userId,
            String occurredAt,
            Map<String, Object> payload
    ) {
    }
}
