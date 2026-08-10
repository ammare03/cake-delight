package com.cakedelight.ratingservice.repository;

import com.cakedelight.ratingservice.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByCakeId(Long cakeId);

    boolean existsByCakeIdAndUserId(Long cakeId, Long userId);

    long countByCakeId(Long cakeId);

    @Query("SELECT AVG(r.ratingValue) FROM Rating r WHERE r.cakeId = :cakeId")
    Double findAverageRatingByCakeId(@Param("cakeId") Long cakeId);
}
