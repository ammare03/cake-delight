package com.cakedelight.ratingservice.exception;

import org.springframework.http.HttpStatus;

/** CLAUDE.md §5.2 — only users who purchased the cake can rate it. */
public class NotPurchasedException extends BusinessException {
    public NotPurchasedException(Long cakeId) {
        super("CAKE_NOT_PURCHASED", HttpStatus.FORBIDDEN, "You can only rate cakes you've purchased (cake " + cakeId + ")");
    }
}
