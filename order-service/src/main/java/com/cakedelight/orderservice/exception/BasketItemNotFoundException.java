package com.cakedelight.orderservice.exception;

public class BasketItemNotFoundException extends RuntimeException {
    public BasketItemNotFoundException(Long itemId) {
        super("No basket item found with id " + itemId);
    }
}
