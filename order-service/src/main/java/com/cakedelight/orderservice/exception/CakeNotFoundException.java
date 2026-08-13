package com.cakedelight.orderservice.exception;

public class CakeNotFoundException extends RuntimeException {
    public CakeNotFoundException(Long cakeId) {
        super("No cake found with id " + cakeId);
    }
}
