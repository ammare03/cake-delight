package com.cakedelight.orderservice.exception;

/** catalog-service returned 404 for the cakeId being added to the basket. */
public class CakeNotFoundException extends RuntimeException {
    public CakeNotFoundException(Long cakeId) {
        super("No cake found with id " + cakeId);
    }
}
