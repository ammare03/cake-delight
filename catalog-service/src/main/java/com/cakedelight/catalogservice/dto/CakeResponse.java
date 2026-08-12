package com.cakedelight.catalogservice.dto;

import com.cakedelight.catalogservice.entity.Cake;

import java.math.BigDecimal;
import java.time.Instant;

public record CakeResponse(
        Long id,
        String name,
        String description,
        String category,
        BigDecimal price,
        Boolean available,
        String imageUrl,
        Instant createdAt
) {
    public static CakeResponse from(Cake cake) {
        return new CakeResponse(
                cake.getId(),
                cake.getName(),
                cake.getDescription(),
                cake.getCategory(),
                cake.getPrice(),
                cake.getAvailable(),
                cake.getImageUrl(),
                cake.getCreatedAt()
        );
    }
}
