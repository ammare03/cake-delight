package com.cakedelight.authservice.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String email,
        String role
) {}
