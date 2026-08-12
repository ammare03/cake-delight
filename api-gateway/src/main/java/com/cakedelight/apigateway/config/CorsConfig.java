package com.cakedelight.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows frontend-service (a different origin than the gateway) to call the
 * API from a browser. No Spring Security is on the classpath here — this
 * gateway's trust boundary is {@link com.cakedelight.apigateway.security.JwtAuthenticationFilter},
 * a plain servlet filter — so plain WebMvcConfigurer CORS is enough; there's
 * no SecurityFilterChain CORS config to keep in sync with it. This is the one
 * @Configuration class in the whole project's business/gateway layer — CORS
 * is the one genuinely frontend-facing concern, config-driven so a deployed
 * frontend's origin is a config-repo edit, not a gateway rebuild.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.frontend.origin:http://localhost:3000}")
    private String frontendOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type");
    }
}
