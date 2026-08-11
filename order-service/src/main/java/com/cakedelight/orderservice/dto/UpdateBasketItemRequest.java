package com.cakedelight.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateBasketItemRequest(
        @NotNull(message = "quantity is required") @Positive(message = "quantity must be positive") Integer quantity
) {}
