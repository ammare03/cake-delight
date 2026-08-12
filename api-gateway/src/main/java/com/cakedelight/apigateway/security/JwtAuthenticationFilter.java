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

/**
 * The gateway's trust boundary (CLAUDE.md §4). Runs as a plain servlet
 * Filter — this is the servlet/MVC-based gateway variant
 * (spring-cloud-starter-gateway-server-webmvc), not the reactive one, so
 * there's no GlobalFilter/WebFilter chain to hook into; a regular
 * OncePerRequestFilter applies to every request reaching the embedded
 * Tomcat regardless of how the route itself was declared.
 *
 * Public routes skip validation entirely — either unconditionally
 * ({@code app.security.public-paths}) or for GET only
 * ({@code app.security.public-get-paths}, e.g. catalog browsing, where
 * mutating verbs on the same path still need a token). Everything else
 * needs a valid Bearer token; on success, X-User-Id / X-User-Role are
 * injected so downstream services can trust them without re-validating the
 * JWT.
 */
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

    // The key is rebuilt from the config-loaded secret on each use rather than
    // cached at startup — one less lifecycle method to explain, and building an
    // HMAC key from a short string is cheap even on the gateway's hottest path.
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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

        // CORS preflight (Phase 5 — frontend-service is a cross-origin
        // caller now, see CorsConfig) must never require a token: browsers
        // send OPTIONS without credentials or an Authorization header by
        // design, on every path, not just the public ones. This servlet
        // Filter runs ahead of DispatcherServlet, so without this check
        // every preflight for a protected path (i.e. anything but
        // register/login) would 401 here before Spring MVC's own CORS
        // handling (registered via CorsConfig's addCorsMappings) ever got a
        // chance to answer it — silently breaking the *actual* request too,
        // since browsers refuse to send it after a failed preflight. Not a
        // security hole: this filter still never forwards OPTIONS anywhere
        // that performs a mutation, since it carries no body.
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
            // Phase 4: order-service embeds this in the order.completed event
            // payload so notification-service is self-contained and never
            // needs to call back to auth-service for it (CLAUDE.md §5.3).
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
