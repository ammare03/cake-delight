package com.cakedelight.catalogservice.dto.response;

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
) {}
