package com.cakedelight.catalogservice.exception;

public class CakeNotFoundException extends RuntimeException {
    public CakeNotFoundException(Long id) {
        super("No cake found with id " + id);
    }
}
