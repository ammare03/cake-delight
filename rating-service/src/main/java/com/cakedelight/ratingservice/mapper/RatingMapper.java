package com.cakedelight.ratingservice.mapper;

import com.cakedelight.ratingservice.dto.request.CreateRatingRequest;
import com.cakedelight.ratingservice.dto.response.RatingResponse;
import com.cakedelight.ratingservice.entity.Rating;
import org.springframework.stereotype.Component;

// Manual mapping, not MapStruct — see catalog-service's CakeMapper for the
// same reasoning; consistent across services.
@Component
public class RatingMapper {

    public Rating toEntity(CreateRatingRequest request, Long userId) {
        Rating rating = new Rating();
        rating.setCakeId(request.cakeId());
        rating.setUserId(userId);
        rating.setRatingValue(request.ratingValue());
        rating.setReviewText(request.reviewText());
        return rating;
    }

    public RatingResponse toResponse(Rating rating) {
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
