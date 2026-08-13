package com.cakedelight.ratingservice.exception;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("Missing or invalid X-User-Id");
    }
}
