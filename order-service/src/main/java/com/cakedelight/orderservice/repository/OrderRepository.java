package com.cakedelight.orderservice.repository;

import com.cakedelight.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndItems_CakeId(Long userId, Long cakeId);
}
