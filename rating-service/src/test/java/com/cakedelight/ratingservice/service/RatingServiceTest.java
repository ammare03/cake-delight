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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    RatingRepository ratingRepository;

    @Mock
    OrderClient orderClient;

    @InjectMocks
    RatingService ratingService;

    @Test
    void submitRating_whenUserIdHeaderMissing_throwsUnauthenticatedException() {
        CreateRatingRequest request = new CreateRatingRequest(1L, 5, "Great cake");

        assertThatThrownBy(() -> ratingService.submitRating(request, null))
                .isInstanceOf(UnauthenticatedException.class);

        verify(orderClient, never()).checkPurchase(any(), any());
        verify(ratingRepository, never()).save(any(Rating.class));
    }

    @Test
    void submitRating_whenUserIdHeaderNotNumeric_throwsUnauthenticatedException() {
        CreateRatingRequest request = new CreateRatingRequest(1L, 5, "Great cake");

        assertThatThrownBy(() -> ratingService.submitRating(request, "not-a-number"))
                .isInstanceOf(UnauthenticatedException.class);

        verify(ratingRepository, never()).save(any(Rating.class));
    }

    @Test
    void submitRating_whenNotPurchased_throwsNotPurchasedException() {
        CreateRatingRequest request = new CreateRatingRequest(1L, 5, "Great cake");
        when(orderClient.checkPurchase(42L, 1L)).thenReturn(new PurchaseCheckResponse(false));

        assertThatThrownBy(() -> ratingService.submitRating(request, "42"))
                .isInstanceOf(NotPurchasedException.class);

        verify(ratingRepository, never()).save(any(Rating.class));
    }

    @Test
    void submitRating_whenOrderServiceUnreachable_throwsOrderServiceUnavailableException() {
        CreateRatingRequest request = new CreateRatingRequest(1L, 5, "Great cake");
        when(orderClient.checkPurchase(42L, 1L)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> ratingService.submitRating(request, "42"))
                .isInstanceOf(OrderServiceUnavailableException.class);

        verify(ratingRepository, never()).save(any(Rating.class));
    }

    @Test
    void submitRating_whenAlreadyRatedByThisUser_throwsDuplicateRatingException() {
        CreateRatingRequest request = new CreateRatingRequest(1L, 5, "Great cake");
        when(orderClient.checkPurchase(42L, 1L)).thenReturn(new PurchaseCheckResponse(true));
        when(ratingRepository.existsByCakeIdAndUserId(1L, 42L)).thenReturn(true);

        assertThatThrownBy(() -> ratingService.submitRating(request, "42"))
                .isInstanceOf(DuplicateRatingException.class);

        verify(ratingRepository, never()).save(any(Rating.class));
    }

    @Test
    void submitRating_whenNewRating_savesAndReturnsResponse() {
        CreateRatingRequest request = new CreateRatingRequest(1L, 5, "Great cake");
        when(orderClient.checkPurchase(42L, 1L)).thenReturn(new PurchaseCheckResponse(true));
        when(ratingRepository.existsByCakeIdAndUserId(1L, 42L)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
            Rating r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        RatingResponse result = ratingService.submitRating(request, "42");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.cakeId()).isEqualTo(1L);
        assertThat(result.ratingValue()).isEqualTo(5);
    }

    @Test
    void listRatingsForCake_returnsMappedResponses() {
        Rating rating = new Rating();
        rating.setId(1L);
        rating.setCakeId(1L);
        rating.setUserId(42L);
        rating.setRatingValue(5);
        when(ratingRepository.findByCakeId(1L)).thenReturn(List.of(rating));

        List<RatingResponse> result = ratingService.listRatingsForCake(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getSummary_whenNoRatingsExist_returnsZeroWithoutQueryingAverage() {
        when(ratingRepository.countByCakeId(1L)).thenReturn(0L);

        RatingSummaryResponse result = ratingService.getSummary(1L);

        assertThat(result.averageRating()).isEqualTo(0.0);
        assertThat(result.totalRatings()).isEqualTo(0L);
        verify(ratingRepository, never()).findAverageRatingByCakeId(any());
    }

    @Test
    void getSummary_whenRatingsExist_returnsRoundedAverage() {
        when(ratingRepository.countByCakeId(1L)).thenReturn(3L);
        when(ratingRepository.findAverageRatingByCakeId(1L)).thenReturn(4.3333);

        RatingSummaryResponse result = ratingService.getSummary(1L);

        assertThat(result.averageRating()).isEqualTo(4.33);
        assertThat(result.totalRatings()).isEqualTo(3L);
    }
}
