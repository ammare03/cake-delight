package com.cakedelight.orderservice.entity;

// Two states, not one: CREATED at persistence, COMPLETED once the checkout
// transaction is guaranteed durable (OrderService.checkout()). Checkout
// has no further fulfillment workflow to model (CLAUDE.md §12 — no payment
// gateway, no shipping) so COMPLETED is the terminal state — but "maintain
// order status" (CLAUDE.md §5.2) means the column has to actually move at
// least once, not sit permanently on one literal value. Add
// CANCELLED/REFUNDED etc. only if a later phase introduces the workflow
// that needs them.
public enum OrderStatus {
    CREATED,
    COMPLETED
}
