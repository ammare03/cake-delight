package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.dto.response.NotificationResponse;
import com.cakedelight.notificationservice.event.OrderCompletedEvent;

import java.util.List;

public interface NotificationService {

    /**
     * Records (and, for now, only logs) a notification for an order.completed
     * event. Must be safe to call twice with the same event — Kafka's
     * at-least-once delivery means a redelivery is expected, not exceptional
     * (CLAUDE.md §5.3).
     */
    void recordOrderCompleted(OrderCompletedEvent event);

    List<NotificationResponse> listNotifications(String rawUserId);
}
