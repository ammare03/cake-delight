package com.cakedelight.orderservice.dto;

import com.cakedelight.orderservice.entity.BasketItem;

import java.math.BigDecimal;

public record BasketItemResponse(
        Long id,
        Long cakeId,
        String cakeName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {
    public static BasketItemResponse from(BasketItem item) {
        BigDecimal lineTotal = item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new BasketItemResponse(
                item.getId(),
                item.getCakeId(),
                item.getCakeNameSnapshot(),
                item.getUnitPriceSnapshot(),
                item.getQuantity(),
                lineTotal
        );
    }
}
