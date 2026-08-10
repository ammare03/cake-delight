package com.cakedelight.orderservice.exception;

import org.springframework.http.HttpStatus;

/** catalog-service returned 404 for the cakeId being added to the basket. */
public class CakeNotFoundException extends BusinessException {
    public CakeNotFoundException(Long cakeId) {
        super("CAKE_NOT_FOUND", HttpStatus.NOT_FOUND, "No cake found with id " + cakeId);
    }
}
