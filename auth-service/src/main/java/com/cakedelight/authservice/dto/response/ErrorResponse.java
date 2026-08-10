package com.cakedelight.authservice.dto.response;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** Standard error shape for every response this service returns — see the api-conventions skill. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<ValidationFieldError> fieldErrors
) {

    public static ErrorResponse of(HttpStatus status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path, null);
    }

    public static ErrorResponse validation(List<ValidationFieldError> fieldErrors, String path) {
        return new ErrorResponse(Instant.now(), 400, "Bad Request", "VALIDATION_FAILED",
                "Invalid request body", path, fieldErrors);
    }

    /** One field-level validation failure. Nested here because it only ever appears inside this response. */
    public record ValidationFieldError(String field, String message) {}
}
