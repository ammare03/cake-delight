package com.cakedelight.notificationservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when X-User-Id is missing or unparseable — /notifications requires
 * auth at the gateway, so this should only ever trigger when the service is
 * hit directly, bypassing the gateway. Same pattern as rating-service's/
 * order-service's UnauthenticatedException.
 */
public class UnauthenticatedException extends BusinessException {
    public UnauthenticatedException() {
        super("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Missing or invalid caller identity");
    }
}
