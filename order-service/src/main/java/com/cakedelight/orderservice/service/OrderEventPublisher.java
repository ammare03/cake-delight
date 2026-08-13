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

        kafkaTemplate.send(topic, String.valueOf(order.getId()), event);
        log.info("Published order.completed for order {} (user {})", order.getId(), order.getUserId());
    }
}
