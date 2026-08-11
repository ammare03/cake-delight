package com.cakedelight.orderservice.exception;

/**
 * Thrown when X-User-Id is missing or unparseable — every /orders/** route
 * requires auth at the gateway (CLAUDE.md §4 doesn't list any as public), so
 * this should only ever trigger when the service is hit directly, bypassing
 * the gateway.
 */
public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("Missing or invalid caller identity");
    }
}
