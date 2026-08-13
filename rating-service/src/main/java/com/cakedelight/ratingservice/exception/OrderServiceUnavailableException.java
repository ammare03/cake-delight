package com.cakedelight.ratingservice.exception;

public class OrderServiceUnavailableException extends RuntimeException {
    public OrderServiceUnavailableException() {
        super("Could not reach order-service");
    }
}
