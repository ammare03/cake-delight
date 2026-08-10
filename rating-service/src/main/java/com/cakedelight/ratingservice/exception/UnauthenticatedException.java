package com.cakedelight.ratingservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when X-User-Id is missing or unparseable. Every rating-service
 * route requires auth at the gateway (CLAUDE.md §4), so this should only
 * ever fire when the service is hit directly, bypassing the gateway — it's
 * a defensive guard, not the primary auth mechanism.
 */
public class UnauthenticatedException extends BusinessException {
    public UnauthenticatedException() {
        super("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Missing or invalid X-User-Id");
    }
}
