package com.cakedelight.orderservice.exception;

public class CakeUnavailableException extends RuntimeException {
    public CakeUnavailableException(Long cakeId) {
        super("Cake " + cakeId + " is not currently available");
    }
}
