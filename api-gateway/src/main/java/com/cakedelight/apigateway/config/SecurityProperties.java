package com.cakedelight.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Routes reachable without a JWT (CLAUDE.md §4). Config-driven, not a Java
 * literal in the filter — the same reason routes and the JWT secret live in
 * config-repo/ rather than compiled code: Phase 3 adds another public route
 * (catalog browsing) and that should be a config-repo edit, not a gateway
 * rebuild.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private List<String> publicPaths = List.of();
}
