package com.cakedelight.ratingservice.service;

import com.cakedelight.ratingservice.dto.request.CreateRatingRequest;
import com.cakedelight.ratingservice.dto.response.RatingResponse;
import com.cakedelight.ratingservice.dto.response.RatingSummaryResponse;

import java.util.List;

public interface RatingService {

    RatingResponse submitRating(CreateRatingRequest request, String rawUserId);

    List<RatingResponse> listRatingsForCake(Long cakeId);

    RatingSummaryResponse getSummary(Long cakeId);
}
