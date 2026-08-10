package com.cakedelight.orderservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        BigDecimal totalAmount,
        String status,
        List<OrderItemResponse> items,
        Instant createdAt
) {}
