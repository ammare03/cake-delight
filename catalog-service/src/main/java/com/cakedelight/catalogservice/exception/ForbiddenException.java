package com.cakedelight.catalogservice.exception;

/**
 * Thrown when an authenticated caller's role (from the gateway-injected
 * X-User-Role header) isn't allowed to perform the action — e.g. a
 * non-ADMIN user calling a cake create/update/delete endpoint. The gateway
 * only proves *who* the caller is; this service enforces *what they're
 * allowed to do* (CLAUDE.md §4).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
