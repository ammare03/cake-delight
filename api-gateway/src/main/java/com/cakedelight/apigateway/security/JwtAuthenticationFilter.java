package com.cakedelight.apigateway.security;

import com.cakedelight.apigateway.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.security.public-paths:}")
    private String[] publicPaths;

    @Value("${app.security.public-get-paths:}")
    private String[] publicGetPaths;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        MutableHttpServletRequest wrapped = new MutableHttpServletRequest(request);
        wrapped.putHeader("X-Original-Path", path);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path, request.getMethod())) {
            filterChain.doFilter(wrapped, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            reject(response, request, "Missing or malformed Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            wrapped.putHeader("X-User-Id", claims.getSubject());
            wrapped.putHeader("X-User-Role", claims.get("role", String.class));
            wrapped.putHeader("X-User-Email", claims.get("email", String.class));

            filterChain.doFilter(wrapped, response);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected request to {}: invalid JWT ({})", path, ex.getMessage());
            reject(response, request, "Invalid or expired token");
        }
    }

    private boolean isPublic(String path, String method) {
        if (path.startsWith("/actuator/")) {
            return true;
        }
        if (matchesAny(publicPaths, path)) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && matchesAny(publicGetPaths, path);
    }

    private boolean matchesAny(String[] patterns, String path) {
        for (String pattern : patterns) {
            if (!pattern.isBlank() && pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, request.getRequestURI())));
    }
}
