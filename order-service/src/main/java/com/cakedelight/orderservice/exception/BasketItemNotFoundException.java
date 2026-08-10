package com.cakedelight.orderservice.exception;

import org.springframework.http.HttpStatus;

public class BasketItemNotFoundException extends BusinessException {
    public BasketItemNotFoundException(Long itemId) {
        super("BASKET_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "No basket item found with id " + itemId);
    }
}
