package com.cakedelight.ratingservice.controller;

import com.cakedelight.ratingservice.dto.response.RatingResponse;
import com.cakedelight.ratingservice.dto.response.RatingSummaryResponse;
import com.cakedelight.ratingservice.exception.DuplicateRatingException;
import com.cakedelight.ratingservice.exception.GlobalExceptionHandler;
import com.cakedelight.ratingservice.exception.UnauthenticatedException;
import com.cakedelight.ratingservice.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RatingController.class)
@Import(GlobalExceptionHandler.class)
class RatingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RatingService ratingService;

    @Test
    void submitRating_withOutOfRangeValue_returns400() throws Exception {
        String body = """
                { "cakeId": 1, "ratingValue": 7, "reviewText": "too high" }
                """;

        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void submitRating_withMissingUserIdHeader_returns401() throws Exception {
        when(ratingService.submitRating(any(), isNull())).thenThrow(new UnauthenticatedException());
        String body = """
                { "cakeId": 1, "ratingValue": 5, "reviewText": "Great" }
                """;

        mockMvc.perform(post("/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void submitRating_whenAlreadyRated_returns409() throws Exception {
        when(ratingService.submitRating(any(), eq("42"))).thenThrow(new DuplicateRatingException(1L, 42L));
        String body = """
                { "cakeId": 1, "ratingValue": 5, "reviewText": "Great" }
                """;

        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RATING"));
    }

    @Test
    void submitRating_whenValid_returns201() throws Exception {
        when(ratingService.submitRating(any(), eq("42"))).thenReturn(
                new RatingResponse(1L, 1L, 42L, 5, "Great", Instant.now()));
        String body = """
                { "cakeId": 1, "ratingValue": 5, "reviewText": "Great" }
                """;

        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void listRatingsForCake_returns200_withList() throws Exception {
        when(ratingService.listRatingsForCake(1L)).thenReturn(
                List.of(new RatingResponse(1L, 1L, 42L, 5, "Great", Instant.now())));

        mockMvc.perform(get("/ratings/cakes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ratingValue").value(5));
    }

    @Test
    void getSummary_returns200_withAverageAndTotal() throws Exception {
        when(ratingService.getSummary(1L)).thenReturn(new RatingSummaryResponse(4.5, 2));

        mockMvc.perform(get("/ratings/cakes/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalRatings").value(2));
    }
}
