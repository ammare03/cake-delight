package com.cakedelight.ratingservice.exception;

/**
 * order-service itself couldn't be reached (down, network error, etc.) —
 * distinct from a legitimate "not purchased" answer. A domain exception,
 * not a raw FeignException leaking out of the service layer.
 */
public class OrderServiceUnavailableException extends RuntimeException {
    public OrderServiceUnavailableException() {
        super("Could not reach order-service");
    }
}
