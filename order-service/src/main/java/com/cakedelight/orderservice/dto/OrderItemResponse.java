package com.cakedelight.orderservice.dto;

import com.cakedelight.orderservice.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long cakeId,
        String cakeName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getId(),
                item.getCakeId(),
                item.getCakeName(),
                item.getUnitPrice(),
                item.getQuantity(),
                lineTotal
        );
    }
}
