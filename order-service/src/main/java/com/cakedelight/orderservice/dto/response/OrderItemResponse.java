package com.cakedelight.orderservice.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long cakeId,
        String cakeName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {}
