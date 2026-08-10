package com.cakedelight.notificationservice.dto.response;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long orderId,
        String channel,
        String status,
        String payload,
        Instant createdAt
) {}
