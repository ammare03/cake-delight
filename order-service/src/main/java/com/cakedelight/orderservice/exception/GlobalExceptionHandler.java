package com.cakedelight.orderservice.exception;

import com.cakedelight.orderservice.dto.ErrorResponse;
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

    @ExceptionHandler(BasketItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBasketItemNotFound(BasketItemNotFoundException ex, HttpServletRequest req) {
        log.warn("Basket item not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "BASKET_ITEM_NOT_FOUND", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(CakeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCakeNotFound(CakeNotFoundException ex, HttpServletRequest req) {
        log.warn("Cake not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "CAKE_NOT_FOUND", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(CakeUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCakeUnavailable(CakeUnavailableException ex, HttpServletRequest req) {
        log.warn("Cake unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, "CAKE_UNAVAILABLE", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCatalogUnavailable(CatalogUnavailableException ex, HttpServletRequest req) {
        log.error("catalog-service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(EmptyBasketException.class)
    public ResponseEntity<ErrorResponse> handleEmptyBasket(EmptyBasketException ex, HttpServletRequest req) {
        log.warn("Empty basket: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "BASKET_EMPTY", ex.getMessage(), resolvePath(req)));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex, HttpServletRequest req) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage(), resolvePath(req)));
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

    // See auth-service's GlobalExceptionHandler for the full rationale — the
    // gateway forwards with /api stripped, so req.getRequestURI() here is
    // already the internal post-StripPrefix path. X-Original-Path carries
    // what the client actually called; fall back to the local path when
    // absent (this service hit directly, bypassing the gateway).
    private String resolvePath(HttpServletRequest req) {
        String original = req.getHeader("X-Original-Path");
        return original != null ? original : req.getRequestURI();
    }
}
