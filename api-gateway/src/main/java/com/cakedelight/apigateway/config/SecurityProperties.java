package com.cakedelight.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Routes reachable without a JWT (CLAUDE.md §4). Config-driven, not a Java
 * literal in the filter — the same reason routes and the JWT secret live in
 * config-repo/ rather than compiled code: adding a public route should be a
 * config-repo edit, not a gateway rebuild.
 *
 * Both lists are Ant-style path patterns (matched with {@link org.springframework.util.AntPathMatcher},
 * so {@code /**} works), not exact strings.
 *
 * - {@code publicPaths} — public regardless of HTTP method (e.g. auth
 *   register/login, which only ever receive POST anyway).
 * - {@code publicGetPaths} — public for GET only; any other method on a
 *   matching path (e.g. POST/PUT/DELETE /api/catalog/cakes/**) still needs a
 *   valid token. This is what lets catalog browsing be public while
 *   admin-only mutations on the same path prefix stay behind auth — the
 *   gateway only proves *who* the caller is via X-User-Id/X-User-Role;
 *   whether that role is allowed to write is enforced downstream, in
 *   catalog-service itself (CLAUDE.md §4: the gateway is the identity trust
 *   boundary, not the authorization boundary).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private List<String> publicPaths = List.of();
    private List<String> publicGetPaths = List.of();
}
