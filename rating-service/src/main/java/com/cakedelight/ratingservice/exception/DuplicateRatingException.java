package com.cakedelight.ratingservice.exception;

import org.springframework.http.HttpStatus;

/** One rating per user per cake (CLAUDE.md §5.2 — unique constraint on (cake_id, user_id)). */
public class DuplicateRatingException extends BusinessException {
    public DuplicateRatingException(Long cakeId, Long userId) {
        super("DUPLICATE_RATING", HttpStatus.CONFLICT,
                "User " + userId + " has already rated cake " + cakeId);
    }
}
