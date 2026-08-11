package com.cakedelight.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows frontend-service (a different origin than the gateway) to call the
 * API from a browser. No Spring Security is on the classpath here — this
 * gateway's trust boundary is {@link com.cakedelight.apigateway.security.JwtAuthenticationFilter},
 * a plain servlet filter — so plain WebMvcConfigurer CORS is enough; there's
 * no SecurityFilterChain CORS config to keep in sync with it.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsProperties.getOrigin())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type");
    }
}
