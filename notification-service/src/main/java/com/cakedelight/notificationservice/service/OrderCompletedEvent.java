package com.cakedelight.notificationservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// The order.completed contract, verbatim from CLAUDE.md §5.3 — duplicated
// from order-service's identical producer-side record, not shared (CLAUDE.md
// §10, no shared domain-model JAR across service boundaries). Self-contained
// by design: every field this service needs to record a notification is
// right here, no callback to order-service/catalog-service/auth-service.
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
