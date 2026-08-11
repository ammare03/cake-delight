package com.cakedelight.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The frontend's origin(s), allowed to call the gateway from a browser
 * (Phase 5 — frontend-service runs on a different origin, e.g.
 * http://localhost:3000, than the gateway's :9090). Config-driven for the
 * same reason as {@link SecurityProperties}: changing the allowed origin
 * (e.g. for a deployed frontend) should be a config-repo edit, not a
 * gateway rebuild.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.frontend")
public class CorsProperties {
    private String origin = "http://localhost:3000";
}
