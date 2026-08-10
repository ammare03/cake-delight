package com.cakedelight.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// Plain @Getter/@Setter, not @Data — see catalog-service's Cake entity for
// the JPA equals()/hashCode() reasoning (coding-guidelines §4).
@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(name = "uk_notification_event_id", columnNames = "event_id"))
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References auth-service's users.id by value only, no FK — same
    // reasoning as everywhere else user identity crosses a service boundary.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // References order-service's orders.id by value only, no FK.
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // Not one of CLAUDE.md §5.2's literal columns, but required to satisfy
    // its own §5.3 idempotency rule ("consumers must handle receiving the
    // same event twice ... check by eventId") — this is what that check is
    // actually done against. Unique at the DB level, not just re-checked in
    // NotificationService, same pattern as rating-service's duplicate
    // check on (cake_id, user_id).
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
