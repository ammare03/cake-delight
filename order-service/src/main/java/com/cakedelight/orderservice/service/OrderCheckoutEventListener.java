package com.cakedelight.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires only after the checkout transaction commits — {@code phase =
 * AFTER_COMMIT} is what actually fixes Blocker B1, not just the fact that
 * this is a separate class. If the transaction rolls back, this listener
 * never runs and no Kafka message is ever sent for an order that doesn't
 * durably exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCheckoutEventListener {

    private final OrderEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCheckedOut(OrderCheckedOutEvent event) {
        eventPublisher.publishOrderCompleted(event.order(), event.userEmail());
    }
}
