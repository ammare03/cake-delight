package com.cakedelight.orderservice.dto;

import com.cakedelight.orderservice.entity.Basket;

import java.math.BigDecimal;
import java.util.List;

public record BasketResponse(
        Long id,
        Long userId,
        List<BasketItemResponse> items,
        BigDecimal totalAmount
) {
    public static BasketResponse from(Basket basket) {
        List<BasketItemResponse> items = basket.getItems().stream().map(BasketItemResponse::from).toList();
        BigDecimal total = items.stream()
                .map(BasketItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BasketResponse(basket.getId(), basket.getUserId(), items, total);
    }
}
