package com.cakedelight.orderservice.exception;

import org.springframework.http.HttpStatus;

/**
 * catalog-service itself couldn't be reached (down, network error, etc.) —
 * distinct from CakeNotFoundException/CakeUnavailableException, which mean
 * catalog-service answered but said no. The Feign fallback strategy
 * CLAUDE.md §8 asks for: a domain exception, not a raw FeignException leaking
 * out of the service layer.
 */
public class CatalogUnavailableException extends BusinessException {
    public CatalogUnavailableException() {
        super("CATALOG_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Could not reach catalog-service");
    }
}
