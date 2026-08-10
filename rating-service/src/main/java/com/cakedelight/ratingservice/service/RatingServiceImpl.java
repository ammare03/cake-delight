package com.cakedelight.ratingservice.service;

import com.cakedelight.ratingservice.dto.request.CreateRatingRequest;
import com.cakedelight.ratingservice.dto.response.RatingResponse;
import com.cakedelight.ratingservice.dto.response.RatingSummaryResponse;
import com.cakedelight.ratingservice.entity.Rating;
import com.cakedelight.ratingservice.exception.DuplicateRatingException;
import com.cakedelight.ratingservice.exception.UnauthenticatedException;
import com.cakedelight.ratingservice.mapper.RatingMapper;
import com.cakedelight.ratingservice.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RatingMapper ratingMapper;

    @Override
    @Transactional
    public RatingResponse submitRating(CreateRatingRequest request, String rawUserId) {
        Long userId = parseUserId(rawUserId);

        // TODO(Phase 4): CLAUDE.md §5.2 requires verifying the caller actually
        // purchased this cake before letting them rate it, via a Feign call to
        // order-service. order-service doesn't exist yet (Phase 4), so this
        // check is deliberately deferred — see the Phase 3 planning discussion.
        // Don't add it early; wire it in when order-service is real.

        if (ratingRepository.existsByCakeIdAndUserId(request.cakeId(), userId)) {
            throw new DuplicateRatingException(request.cakeId(), userId);
        }

        Rating saved = ratingRepository.save(ratingMapper.toEntity(request, userId));
        log.info("User {} rated cake {} with {}", userId, saved.getCakeId(), saved.getRatingValue());
        return ratingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponse> listRatingsForCake(Long cakeId) {
        return ratingRepository.findByCakeId(cakeId).stream().map(ratingMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getSummary(Long cakeId) {
        long total = ratingRepository.countByCakeId(cakeId);
        if (total == 0) {
            return new RatingSummaryResponse(0.0, 0);
        }
        Double average = ratingRepository.findAverageRatingByCakeId(cakeId);
        return new RatingSummaryResponse(Math.round(average * 100.0) / 100.0, total);
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
