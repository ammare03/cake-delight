package com.cakedelight.apigateway.security;

import com.cakedelight.apigateway.config.JwtProperties;
import com.cakedelight.apigateway.config.SecurityProperties;
import com.cakedelight.apigateway.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The gateway's trust boundary (CLAUDE.md §4). Runs as a plain servlet
 * Filter — this is the servlet/MVC-based gateway variant
 * (spring-cloud-starter-gateway-server-webmvc), not the reactive one, so
 * there's no GlobalFilter/WebFilter chain to hook into; a regular
 * OncePerRequestFilter applies to every request reaching the embedded
 * Tomcat regardless of how the route itself was declared.
 *
 * Public routes skip validation entirely. Everything else needs a valid
 * Bearer token; on success, X-User-Id / X-User-Role are injected so
 * downstream services can trust them without re-validating the JWT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    // Built once at startup, not per request — this filter runs on every
    // request through the gateway, so rebuilding the key from the config
    // string each time would be wasted work on the busiest path in the system.
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Wrapped unconditionally, before the public/protected branch: every
        // request forwarded downstream — public or authenticated — needs the
        // original pre-StripPrefix path available so downstream error
        // handlers (e.g. auth-service's GlobalExceptionHandler) can report
        // the path the client actually called, not the internal one.
        MutableHttpServletRequest wrapped = new MutableHttpServletRequest(request);
        wrapped.putHeader("X-Original-Path", path);

        if (isPublic(path)) {
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
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            wrapped.putHeader("X-User-Id", claims.getSubject());
            wrapped.putHeader("X-User-Role", claims.get("role", String.class));

            filterChain.doFilter(wrapped, response);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected request to {}: invalid JWT ({})", path, ex.getMessage());
            reject(response, request, "Invalid or expired token");
        }
    }

    private boolean isPublic(String path) {
        return securityProperties.getPublicPaths().contains(path) || path.startsWith("/actuator/");
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, request.getRequestURI())));
    }
}
