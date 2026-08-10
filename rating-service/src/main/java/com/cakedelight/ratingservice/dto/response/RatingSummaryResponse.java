package com.cakedelight.ratingservice.dto.response;

// averageRating is 0.0 when totalRatings is 0 — check totalRatings, not
// averageRating, to tell "no ratings yet" apart from "genuinely rated 0".
public record RatingSummaryResponse(double averageRating, long totalRatings) {}
