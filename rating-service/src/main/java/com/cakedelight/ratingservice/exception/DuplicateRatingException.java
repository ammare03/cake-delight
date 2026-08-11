package com.cakedelight.ratingservice.exception;

/** One rating per user per cake (CLAUDE.md §5.2 — unique constraint on (cake_id, user_id)). */
public class DuplicateRatingException extends RuntimeException {
    public DuplicateRatingException(Long cakeId, Long userId) {
        super("User " + userId + " has already rated cake " + cakeId);
    }
}
