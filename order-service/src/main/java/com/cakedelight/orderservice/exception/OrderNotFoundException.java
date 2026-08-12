package com.cakedelight.orderservice.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("No order found with id " + id);
    }
}
