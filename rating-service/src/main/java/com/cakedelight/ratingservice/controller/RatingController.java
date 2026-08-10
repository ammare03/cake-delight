package com.cakedelight.ratingservice.controller;

import com.cakedelight.ratingservice.dto.request.CreateRatingRequest;
import com.cakedelight.ratingservice.dto.response.RatingResponse;
import com.cakedelight.ratingservice.dto.response.RatingSummaryResponse;
import com.cakedelight.ratingservice.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Every endpoint here requires auth at the gateway (CLAUDE.md §4 doesn't
// list any /api/ratings/** path as public) — X-User-Id is always expected
// to be present by the time a request reaches this controller.
@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @Operation(summary = "Submit a rating for a cake")
    @ApiResponse(responseCode = "201", description = "Rating created")
    @ApiResponse(responseCode = "409", description = "Caller has already rated this cake")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse submitRating(
            @Valid @RequestBody CreateRatingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ratingService.submitRating(request, userId);
    }

    @Operation(summary = "List ratings for a cake")
    @GetMapping("/cakes/{cakeId}")
    public List<RatingResponse> listRatingsForCake(@PathVariable Long cakeId) {
        return ratingService.listRatingsForCake(cakeId);
    }

    @Operation(summary = "Get the average rating and total count for a cake")
    @GetMapping("/cakes/{cakeId}/summary")
    public RatingSummaryResponse getSummary(@PathVariable Long cakeId) {
        return ratingService.getSummary(cakeId);
    }
}
