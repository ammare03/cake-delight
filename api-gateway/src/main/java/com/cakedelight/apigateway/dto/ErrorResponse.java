package com.cakedelight.apigateway.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/** Same shape as every other service's error response — see the api-conventions skill. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path
) {
    public static ErrorResponse of(HttpStatus status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path);
    }
}
