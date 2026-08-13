package com.cakedelight.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
