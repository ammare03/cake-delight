package com.cakedelight.orderservice.exception;

public class EmptyBasketException extends RuntimeException {
    public EmptyBasketException() {
        super("Cannot checkout an empty basket");
    }
}
