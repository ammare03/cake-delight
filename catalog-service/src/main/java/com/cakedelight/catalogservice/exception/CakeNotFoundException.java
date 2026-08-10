package com.cakedelight.catalogservice.exception;

import org.springframework.http.HttpStatus;

public class CakeNotFoundException extends BusinessException {
    public CakeNotFoundException(Long id) {
        super("CAKE_NOT_FOUND", HttpStatus.NOT_FOUND, "No cake found with id " + id);
    }
}
