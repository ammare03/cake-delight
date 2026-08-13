package com.cakedelight.ratingservice.exception;

public class NotPurchasedException extends RuntimeException {
    public NotPurchasedException(Long cakeId) {
        super("You can only rate cakes you've purchased (cake " + cakeId + ")");
    }
}
