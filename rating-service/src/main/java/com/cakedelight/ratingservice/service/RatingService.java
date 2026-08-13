package com.cakedelight.ratingservice.service;

import com.cakedelight.ratingservice.dto.CreateRatingRequest;
import com.cakedelight.ratingservice.dto.RatingResponse;
import com.cakedelight.ratingservice.dto.RatingSummaryResponse;
import com.cakedelight.ratingservice.entity.Rating;
import com.cakedelight.ratingservice.exception.DuplicateRatingException;
import com.cakedelight.ratingservice.exception.NotPurchasedException;
import com.cakedelight.ratingservice.exception.OrderServiceUnavailableException;
import com.cakedelight.ratingservice.exception.UnauthenticatedException;
import com.cakedelight.ratingservice.repository.RatingRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final OrderClient orderClient;

    @Transactional
    public RatingResponse submitRating(CreateRatingRequest request, String rawUserId) {
        Long userId = parseUserId(rawUserId);

        requirePurchased(userId, request.cakeId());

        if (ratingRepository.existsByCakeIdAndUserId(request.cakeId(), userId)) {
            throw new DuplicateRatingException(request.cakeId(), userId);
        }

        Rating rating = new Rating();
        rating.setCakeId(request.cakeId());
        rating.setUserId(userId);
        rating.setRatingValue(request.ratingValue());
        rating.setReviewText(request.reviewText());

        Rating saved = ratingRepository.save(rating);
        log.info("User {} rated cake {} with {}", userId, saved.getCakeId(), saved.getRatingValue());
        return RatingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> listRatingsForCake(Long cakeId) {
        return ratingRepository.findByCakeId(cakeId).stream().map(RatingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getSummary(Long cakeId) {
        long total = ratingRepository.countByCakeId(cakeId);
        if (total == 0) {
            return new RatingSummaryResponse(0.0, 0);
        }
        Double average = ratingRepository.findAverageRatingByCakeId(cakeId);
        return new RatingSummaryResponse(Math.round(average * 100.0) / 100.0, total);
    }

    private void requirePurchased(Long userId, Long cakeId) {
        PurchaseCheckResponse result;
        try {
            result = orderClient.checkPurchase(userId, cakeId);
        } catch (FeignException ex) {
            log.error("order-service call failed while checking purchase for user {} cake {}", userId, cakeId, ex);
            throw new OrderServiceUnavailableException();
        }
        if (!result.purchased()) {
            throw new NotPurchasedException(cakeId);
        }
    }

    private Long parseUserId(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new UnauthenticatedException();
        }
        try {
            return Long.parseLong(rawUserId);
        } catch (NumberFormatException ex) {
            throw new UnauthenticatedException();
        }
    }
}
