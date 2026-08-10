package com.cakedelight.ratingservice.service;

import com.cakedelight.ratingservice.client.OrderClient;
import com.cakedelight.ratingservice.client.dto.PurchaseCheckResponse;
import com.cakedelight.ratingservice.dto.request.CreateRatingRequest;
import com.cakedelight.ratingservice.dto.response.RatingResponse;
import com.cakedelight.ratingservice.dto.response.RatingSummaryResponse;
import com.cakedelight.ratingservice.entity.Rating;
import com.cakedelight.ratingservice.exception.DuplicateRatingException;
import com.cakedelight.ratingservice.exception.NotPurchasedException;
import com.cakedelight.ratingservice.exception.OrderServiceUnavailableException;
import com.cakedelight.ratingservice.exception.UnauthenticatedException;
import com.cakedelight.ratingservice.mapper.RatingMapper;
import com.cakedelight.ratingservice.repository.RatingRepository;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock
    RatingRepository ratingRepository;

    @Mock
    RatingMapper ratingMapper;

    @Mock
    OrderClient orderClient;

    @InjectMocks
    RatingServiceImpl ratingService;

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
        Rating toSave = new Rating();
        Rating saved = new Rating();
        saved.setId(1L);
        saved.setCakeId(1L);
        saved.setUserId(42L);
        saved.setRatingValue(5);
        when(ratingMapper.toEntity(request, 42L)).thenReturn(toSave);
        when(ratingRepository.save(toSave)).thenReturn(saved);
        when(ratingMapper.toResponse(saved)).thenReturn(
                new RatingResponse(1L, 1L, 42L, 5, "Great cake", Instant.now()));

        RatingResponse result = ratingService.submitRating(request, "42");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(42L);
    }

    @Test
    void listRatingsForCake_returnsMappedResponses() {
        Rating rating = new Rating();
        rating.setId(1L);
        when(ratingRepository.findByCakeId(1L)).thenReturn(List.of(rating));
        when(ratingMapper.toResponse(rating)).thenReturn(
                new RatingResponse(1L, 1L, 42L, 5, "Great cake", Instant.now()));

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
