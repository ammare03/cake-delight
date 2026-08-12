package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Producer side of CLAUDE.md §5.3. Published synchronously right after the
 * order is saved and the basket is cleared, in the same checkout() call —
 * not via a transactional outbox. That's a deliberate simplification, not an
 * oversight: Saga/event-sourcing-style patterns are explicitly out of scope
 * for this capstone (CLAUDE.md §3), so a checkout that fails between the DB
 * commit and the Kafka send is an accepted, documented gap rather than one
 * solved with more machinery. See README's Phase 4 notes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static final String EVENT_TYPE = "ORDER_COMPLETED";

    private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.order-completed}")
    private String topic;

    public void publishOrderCompleted(Order order, String userEmail) {
        List<OrderCompletedEvent.Item> items = order.getItems().stream()
                .map(item -> new OrderCompletedEvent.Item(
                        item.getCakeId(), item.getCakeName(), item.getQuantity(), item.getUnitPrice()))
                .toList();

        OrderCompletedEvent event = new OrderCompletedEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                Instant.now(),
                order.getId(),
                order.getUserId(),
                userEmail,
                order.getTotalAmount(),
                items
        );

        // Keyed by orderId — keeps all messages for the same order on one
        // partition. Not load-bearing with a single partition, but correct
        // if the topic is ever repartitioned.
        kafkaTemplate.send(topic, String.valueOf(order.getId()), event);
        log.info("Published order.completed for order {} (user {})", order.getId(), order.getUserId());
    }
}
