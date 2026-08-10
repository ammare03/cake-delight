package com.cakedelight.orderservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when X-User-Id is missing or unparseable — every /orders/** route
 * requires auth at the gateway (CLAUDE.md §4 doesn't list any as public), so
 * this should only ever trigger when the service is hit directly, bypassing
 * the gateway. Same pattern as rating-service's UnauthenticatedException.
 */
public class UnauthenticatedException extends BusinessException {
    public UnauthenticatedException() {
        super("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Missing or invalid caller identity");
    }
}
