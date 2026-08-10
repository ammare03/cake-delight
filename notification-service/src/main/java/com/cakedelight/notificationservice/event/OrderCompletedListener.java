package com.cakedelight.notificationservice.event;

import com.cakedelight.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Consumer side of CLAUDE.md §5.3. Spring Boot auto-configures @KafkaListener
// support the moment spring-kafka is on the classpath — no @EnableKafka
// needed. Group id / deserializer settings live in
// config-repo/notification-service.properties.
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletedListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topics.order-completed}")
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("Received order.completed event {} for order {}", event.eventId(), event.orderId());
        notificationService.recordOrderCompleted(event);
    }
}
