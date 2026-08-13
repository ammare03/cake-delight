package com.cakedelight.orderservice.exception;

public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException() {
        super("Could not reach catalog-service");
    }
}
