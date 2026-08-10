package com.cakedelight.ratingservice.client.dto;

// Duplicated from order-service's response DTO of the same name, not shared
// — CLAUDE.md §10 forbids a shared domain-model JAR across service
// boundaries.
public record PurchaseCheckResponse(boolean purchased) {}
