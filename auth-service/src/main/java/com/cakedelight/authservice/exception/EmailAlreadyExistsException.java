package com.cakedelight.authservice.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException(String email) {
        super("EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "An account with email " + email + " already exists");
    }
}
