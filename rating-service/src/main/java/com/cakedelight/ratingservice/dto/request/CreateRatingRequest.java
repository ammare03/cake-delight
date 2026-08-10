package com.cakedelight.ratingservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRatingRequest(
        @NotNull(message = "cakeId is required")
        Long cakeId,

        @NotNull(message = "ratingValue is required")
        @Min(value = 1, message = "ratingValue must be between 1 and 5")
        @Max(value = 5, message = "ratingValue must be between 1 and 5")
        Integer ratingValue,

        @Size(max = 2000, message = "reviewText must be at most 2000 characters")
        String reviewText
) {}
