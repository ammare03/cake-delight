package com.cakedelight.orderservice.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(Long id) {
        super("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "No order found with id " + id);
    }
}
