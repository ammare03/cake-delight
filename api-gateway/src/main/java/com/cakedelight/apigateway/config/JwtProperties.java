package com.cakedelight.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Only the secret — the gateway verifies tokens, it doesn't issue them.
 * auth-service's copy of this class carries expirationMs/issuer too, because
 * it actually needs them to issue tokens; the gateway doesn't check issuer
 * and jjwt already validates `exp` during parseSignedClaims(), so those two
 * fields would just be unused config here. Still a separate class per
 * service (no shared JAR — CLAUDE.md §10), just trimmed to what's read.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
}
