package com.cakedelight.apigateway.security;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        filter = new JwtAuthenticationFilter(new ObjectMapper().findAndRegisterModules());

        ReflectionTestUtils.setField(filter, "secret", "test-only-signing-secret-at-least-32-bytes-long");
        ReflectionTestUtils.setField(filter, "publicPaths", new String[] {"/api/auth/register", "/api/auth/login"});
        ReflectionTestUtils.setField(filter, "publicGetPaths", new String[] {"/api/catalog/cakes/**"});

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

    @Test
    void doFilter_optionsOnProtectedPath_withNoToken_isAllowedThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/orders/basket");
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
