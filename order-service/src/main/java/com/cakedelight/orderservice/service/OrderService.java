package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse checkout(String rawUserId, String userEmail);

    List<OrderResponse> listOrders(String rawUserId);

    OrderResponse getOrder(String rawUserId, Long orderId);

    // Internal — called from InternalOrderController, not the user-facing
    // /orders/** endpoints, so it takes userId directly rather than a raw
    // header (there's no caller identity header to parse; the caller is
    // rating-service itself, via Feign).
    boolean hasPurchased(Long userId, Long cakeId);
}
