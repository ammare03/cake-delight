package com.cakedelight.ratingservice.exception;

import org.springframework.http.HttpStatus;

/**
 * order-service itself couldn't be reached (down, network error, etc.) —
 * distinct from a legitimate "not purchased" answer. Same fallback-strategy
 * reasoning as order-service's own CatalogUnavailableException
 * (coding-guidelines §8): a domain exception, not a raw FeignException
 * leaking out of the service layer.
 */
public class OrderServiceUnavailableException extends BusinessException {
    public OrderServiceUnavailableException() {
        super("ORDER_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Could not reach order-service");
    }
}
