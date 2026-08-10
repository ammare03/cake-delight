package com.cakedelight.orderservice.mapper;

import com.cakedelight.orderservice.dto.response.OrderItemResponse;
import com.cakedelight.orderservice.dto.response.OrderResponse;
import com.cakedelight.orderservice.entity.Order;
import com.cakedelight.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(this::toItemResponse).toList();
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus().name(),
                items,
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
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
