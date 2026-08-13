package com.cakedelight.ratingservice.exception;

import com.cakedelight.ratingservice.dto.ErrorResponse;
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

    @ExceptionHandler(DuplicateRatingException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateRatingException ex, HttpServletRequest req) {
        log.warn("Duplicate rating: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, "DUPLICATE_RATING", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(NotPurchasedException.class)
    public ResponseEntity<ErrorResponse> handleNotPurchased(NotPurchasedException ex, HttpServletRequest req) {
        log.warn("Not purchased: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, "CAKE_NOT_PURCHASED", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(OrderServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleOrderServiceUnavailable(OrderServiceUnavailableException ex, HttpServletRequest req) {
        log.error("order-service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_SERVICE_UNAVAILABLE", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException ex, HttpServletRequest req) {
        log.warn("Unauthenticated: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.ValidationFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.ValidationFieldError(f.getField(), f.getDefaultMessage()))
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

    private String resolvePath(HttpServletRequest req) {
        String original = req.getHeader("X-Original-Path");
        return original != null ? original : req.getRequestURI();
    }
}
