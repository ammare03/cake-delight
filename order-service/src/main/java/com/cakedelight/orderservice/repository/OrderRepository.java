package com.cakedelight.orderservice.repository;

import com.cakedelight.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    // Scoped by userId, not just id — a user must never be able to fetch
    // another user's order details by guessing an id.
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    // Backs the internal purchase-verification check rating-service calls
    // via Feign (CLAUDE.md §5.2 — "only users who purchased the cake can
    // rate"). Property-path traversal through the items collection to
    // OrderItem.cakeId.
    boolean existsByUserIdAndItems_CakeId(Long userId, Long cakeId);
}
