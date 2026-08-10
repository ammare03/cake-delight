package com.cakedelight.orderservice.exception;

import org.springframework.http.HttpStatus;

/**
 * The cake exists but catalog-service reports it as not available — the
 * basket-add-time enforcement of the "available" signal that catalog-service's
 * own browse endpoint applies (M1 in the Phase 3 audit; see catalog-service's
 * CakeSpecifications.isAvailable()). A cake that's unavailable can still be
 * fetched directly by id, so this has to be checked here rather than assumed.
 */
public class CakeUnavailableException extends BusinessException {
    public CakeUnavailableException(Long cakeId) {
        super("CAKE_UNAVAILABLE", HttpStatus.CONFLICT, "Cake " + cakeId + " is not currently available");
    }
}
