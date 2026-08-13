package com.cakedelight.notificationservice.exception;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("Missing or invalid caller identity");
    }
}
