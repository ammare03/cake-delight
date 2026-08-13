package com.cakedelight.orderservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCompletedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal totalAmount,
        List<Item> items
) {
    public record Item(Long cakeId, String cakeName, Integer quantity, BigDecimal unitPrice) {}
}
