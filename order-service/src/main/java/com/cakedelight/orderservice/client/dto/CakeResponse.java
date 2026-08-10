package com.cakedelight.orderservice.client.dto;

import java.math.BigDecimal;
import java.time.Instant;

// Duplicated from catalog-service's response DTO of the same name, not
// shared — CLAUDE.md §10 forbids a shared domain-model JAR across service
// boundaries. Trimmed to only what order-service actually reads isn't worth
// doing here since Feign just deserializes whatever JSON catalog-service
// sends; keeping every field matches catalog-service's contract 1:1.
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
