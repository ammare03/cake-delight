package com.cakedelight.orderservice.exception;

import org.springframework.http.HttpStatus;

public class EmptyBasketException extends BusinessException {
    public EmptyBasketException() {
        super("BASKET_EMPTY", HttpStatus.BAD_REQUEST, "Cannot checkout an empty basket");
    }
}
