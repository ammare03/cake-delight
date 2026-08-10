package com.cakedelight.ratingservice.dto.response;

import java.time.Instant;

public record RatingResponse(
        Long id,
        Long cakeId,
        Long userId,
        Integer ratingValue,
        String reviewText,
        Instant createdAt
) {}
