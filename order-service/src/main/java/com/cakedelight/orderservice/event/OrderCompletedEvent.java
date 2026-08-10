package com.cakedelight.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// The order.completed contract, verbatim from CLAUDE.md §5.3. Self-contained
// by design — notification-service deserializes its own identical copy of
// this record and never calls back to order-service/catalog-service/
// auth-service for anything it needs.
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
