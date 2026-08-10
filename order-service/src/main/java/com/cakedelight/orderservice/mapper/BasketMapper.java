package com.cakedelight.orderservice.mapper;

import com.cakedelight.orderservice.dto.response.BasketItemResponse;
import com.cakedelight.orderservice.dto.response.BasketResponse;
import com.cakedelight.orderservice.entity.Basket;
import com.cakedelight.orderservice.entity.BasketItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Manual mapping, not MapStruct — see catalog-service's CakeMapper for the
// same reasoning; consistent across services.
@Component
public class BasketMapper {

    public BasketResponse toResponse(Basket basket) {
        List<BasketItemResponse> items = basket.getItems().stream().map(this::toItemResponse).toList();
        BigDecimal total = items.stream()
                .map(BasketItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BasketResponse(basket.getId(), basket.getUserId(), items, total);
    }

    private BasketItemResponse toItemResponse(BasketItem item) {
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
