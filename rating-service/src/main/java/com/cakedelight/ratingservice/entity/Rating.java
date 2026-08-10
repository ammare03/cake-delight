package com.cakedelight.ratingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

// Plain @Getter/@Setter, not @Data — see coding-guidelines §4 (entity
// equals()/hashCode() from @Data breaks badly with JPA).
@Entity
@Table(name = "ratings", uniqueConstraints = {
        // One rating per user per cake (CLAUDE.md §5.2).
        @UniqueConstraint(name = "uk_rating_cake_user", columnNames = {"cake_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // No FK to catalog-service's cakes table — cross-service DB access is
    // forbidden (CLAUDE.md §10). This service doesn't validate the cake
    // exists; that's out of Phase 3 scope (would need a Feign call).
    @Column(name = "cake_id", nullable = false)
    private Long cakeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rating_value", nullable = false)
    private Integer ratingValue;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
