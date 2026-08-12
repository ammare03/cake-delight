package com.cakedelight.ratingservice.exception;

/** CLAUDE.md §5.2 — only users who purchased the cake can rate it. */
public class NotPurchasedException extends RuntimeException {
    public NotPurchasedException(Long cakeId) {
        super("You can only rate cakes you've purchased (cake " + cakeId + ")");
    }
}
