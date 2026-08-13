package com.cakedelight.orderservice.exception;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("Missing or invalid caller identity");
    }
}
