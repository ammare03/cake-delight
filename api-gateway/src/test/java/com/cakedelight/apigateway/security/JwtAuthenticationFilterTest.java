package com.cakedelight.apigateway.security;

import com.cakedelight.apigateway.config.JwtProperties;
import com.cakedelight.apigateway.config.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// GET-only public paths are a Phase 3 addition (catalog browsing public,
// admin mutations on the same path still gated) — see SecurityProperties.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain filterChain;

    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-only-signing-secret-at-least-32-bytes-long");

        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setPublicPaths(List.of("/api/auth/register", "/api/auth/login"));
        securityProperties.setPublicGetPaths(List.of("/api/catalog/cakes/**"));

        // findAndRegisterModules() picks up jackson-datatype-jsr310 (on the
        // classpath transitively via spring-boot-starter-json) so ErrorResponse's
        // Instant timestamp serializes — matching what Spring's auto-configured
        // ObjectMapper bean does for real in the running app.
        filter = new JwtAuthenticationFilter(jwtProperties, securityProperties, new ObjectMapper().findAndRegisterModules());
        filter.init();

        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @Test
    void doFilter_getOnCatalogBrowsePath_withNoToken_isAllowedThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/catalog/cakes/5");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void doFilter_postOnSameCatalogPath_withNoToken_isRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/catalog/cakes");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void doFilter_exactPublicPath_anyMethod_isAllowedThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void doFilter_getOnUnrelatedProtectedPath_withNoToken_isRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/orders");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // Phase 5 regression: frontend-service is a cross-origin caller now
    // (CorsConfig), and browsers never attach an Authorization header to a
    // CORS preflight — rejecting OPTIONS here would 401 the preflight and
    // silently block the real request on every protected route, not just
    // this one path.
    @Test
    void doFilter_optionsOnProtectedPath_withNoToken_isAllowedThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/orders/basket");
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
