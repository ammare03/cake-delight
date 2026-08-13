package com.cakedelight.ratingservice.exception;

public class DuplicateRatingException extends RuntimeException {
    public DuplicateRatingException(Long cakeId, Long userId) {
        super("User " + userId + " has already rated cake " + cakeId);
    }
}
