package com.cakedelight.catalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// PUT is a full replace (api-conventions §2 — prefer PUT over PATCH), so this
// carries the same required fields as CreateCakeRequest rather than optional
// partial-update fields.
public record UpdateCakeRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "description is required")
        String description,

        @NotBlank(message = "category is required")
        @Size(max = 50, message = "category must be at most 50 characters")
        String category,

        @NotNull(message = "price is required")
        @Positive(message = "price must be positive")
        BigDecimal price,

        @NotNull(message = "available is required")
        Boolean available,

        String imageUrl
) {}
