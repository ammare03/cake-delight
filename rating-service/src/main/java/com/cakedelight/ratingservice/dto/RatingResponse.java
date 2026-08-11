package com.cakedelight.ratingservice.dto;

import com.cakedelight.ratingservice.entity.Rating;

import java.time.Instant;

public record RatingResponse(
        Long id,
        Long cakeId,
        Long userId,
        Integer ratingValue,
        String reviewText,
        Instant createdAt
) {
    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getCakeId(),
                rating.getUserId(),
                rating.getRatingValue(),
                rating.getReviewText(),
                rating.getCreatedAt()
        );
    }
}
