package com.cakedelight.authservice.exception;

import com.cakedelight.authservice.dto.response.ErrorResponse;
import com.cakedelight.authservice.dto.response.ValidationFieldError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("Business error {}: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), ex.getCode(), ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ValidationFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ValidationFieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.validation(errors, resolvePath(req)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error handling {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "Something went wrong", resolvePath(req)));
    }

    // The gateway forwards requests with /api stripped (CLAUDE.md §4), so by
    // the time they reach this service, req.getRequestURI() is already the
    // internal post-StripPrefix path. The gateway injects X-Original-Path
    // with the path the client actually called; prefer that when present.
    // Falls back to req.getRequestURI() so error responses stay sensible
    // when this service is hit directly, bypassing the gateway, during
    // local dev/testing (still possible pre-Phase-6).
    private String resolvePath(HttpServletRequest req) {
        String original = req.getHeader("X-Original-Path");
        return original != null ? original : req.getRequestURI();
    }
}
