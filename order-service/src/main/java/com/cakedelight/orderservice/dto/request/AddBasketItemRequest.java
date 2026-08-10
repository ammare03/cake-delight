package com.cakedelight.orderservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddBasketItemRequest(
        @NotNull(message = "cakeId is required") Long cakeId,
        @NotNull(message = "quantity is required") @Positive(message = "quantity must be positive") Integer quantity
) {}
