package com.cakedelight.orderservice.exception;

/**
 * The cake exists but catalog-service reports it as not available — a cake
 * that's unavailable can still be fetched directly by id, so this has to be
 * checked here rather than assumed.
 */
public class CakeUnavailableException extends RuntimeException {
    public CakeUnavailableException(Long cakeId) {
        super("Cake " + cakeId + " is not currently available");
    }
}
