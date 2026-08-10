package com.cakedelight.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BasketResponse(
        Long id,
        Long userId,
        List<BasketItemResponse> items,
        BigDecimal totalAmount
) {}
