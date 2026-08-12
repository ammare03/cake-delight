package com.cakedelight.notificationservice.exception;

/**
 * Thrown when X-User-Id is missing or unparseable — /notifications requires
 * auth at the gateway, so this should only ever trigger when the service is
 * hit directly, bypassing the gateway.
 */
public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("Missing or invalid caller identity");
    }
}
