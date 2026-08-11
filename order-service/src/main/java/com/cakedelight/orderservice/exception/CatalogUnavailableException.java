package com.cakedelight.orderservice.exception;

/**
 * catalog-service itself couldn't be reached (down, network error, etc.) —
 * distinct from CakeNotFoundException/CakeUnavailableException, which mean
 * catalog-service answered but said no.
 */
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException() {
        super("Could not reach catalog-service");
    }
}
