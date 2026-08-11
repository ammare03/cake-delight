package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.entity.Order;

/**
 * Internal Spring application event — not the Kafka {@link OrderCompletedEvent}.
 * Published from {@code OrderService.checkout()} while its transaction is
 * still open; {@link OrderCheckoutEventListener} picks it up with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} so the Kafka send
 * only ever happens once the order is durably committed (Audit 2026-08-10
 * Phase 4, Blocker B1 — publishing from inside the transaction risked
 * notifying a customer about an order that then rolled back).
 */
public record OrderCheckedOutEvent(Order order, String userEmail) {}
