package com.cakedelight.notificationservice.dto;

import com.cakedelight.notificationservice.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long orderId,
        String channel,
        String status,
        String payload,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getOrderId(),
                notification.getChannel().name(),
                notification.getStatus().name(),
                notification.getPayload(),
                notification.getCreatedAt()
        );
    }
}
