# Cake Delight

A cloud-native cake e-commerce app built as 8 Spring Boot microservices + a Next.js frontend. Capstone project — see `docs/audits/` for progress audits against the requirements.

**Status:** Phase 2 (Auth Service) — `config-server`, `eureka-server`, `api-gateway`, and `auth-service` are up and verified working end-to-end: register/login through the gateway issues a real JWT, and the gateway rejects missing/invalid tokens on any route not explicitly marked public. Remaining business services (`cake-catalog-service`, `order-service`, `rating-service`, `notification-service`) and the frontend are not started yet.

## Prerequisites

- JDK 17
- Maven 3.9+ (each service also ships its own `mvnw` wrapper)
- A MySQL-compatible server reachable locally (see **Local database** below) — needed from `auth-service` onward
- Docker Desktop (for the bundled MySQL via `docker-compose`, and later Kafka from Phase 4)
- Node.js 20+ (for the frontend, from Phase 5 onward)
- IntelliJ IDEA (or any IDE with Spring Boot support)

## Services and ports

| Service | Port | Purpose |
|---|---|---|
| `config-server` | 8888 | Centralized config, serves everything from `config-repo/` |
| `eureka-server` | 8761 | Service registry — dashboard at `http://localhost:8761` |
| `auth-service` | 8081 | Registration, login, JWT issuance |
| `api-gateway` | **9090** | Single public entry point |

> **Note:** `api-gateway` runs on `9090`, not the more conventional `8080`. This is a local port conflict (XAMPP occupies `8080` on the primary dev machine), not a design decision — if your environment doesn't have that conflict, the port is still `9090` per `config-repo/api-gateway.properties`; change it there if you'd rather use `8080`.

Remaining services aren't built yet; their planned ports are documented in `CLAUDE.md` (not committed — internal dev/agent notes) and will be added here as each one ships.

## Startup order

Services must start in this order — each one depends on the previous being up:

```
1. docker-compose up -d mysql   (only if not using an existing local MySQL — see Local database)
2. config-server                (mvn spring-boot:run, or run the main class in your IDE)
3. eureka-server
4. auth-service                 (and any other business service, in any order among themselves)
5. api-gateway                  (last, so it can resolve lb:// routes to already-registered services)
```

Wait for each service to fully start (watch for `Started XxxApplication` in the console) before starting the next.

Once running:
- Config server: `http://localhost:8888/auth-service/default` (or any service name) — should return that service's resolved config as JSON.
- Eureka dashboard: `http://localhost:8761` — should show `AUTH-SERVICE` and `API-GATEWAY` registered once both are up.
- Register: `POST http://localhost:9090/api/auth/register` with `{"email": "...", "password": "..."}` (password ≥ 8 chars) — returns `201` and a JWT.
- Login: `POST http://localhost:9090/api/auth/login` with the same body — returns `200` and a JWT.
- Anything else through the gateway needs `Authorization: Bearer <token>` — a missing or invalid token gets a `401` in the standard error shape (see `api-conventions` skill).

## Authentication & identity propagation

The gateway is the trust boundary — it's the only service that validates the JWT; everything downstream trusts what the gateway tells it.

1. `auth-service` issues an HS256 JWT on register/login, containing `sub` (user id), `email`, `role`, `iss`, `iat`, `exp`.
2. Every request through `api-gateway` passes through a servlet filter (`JwtAuthenticationFilter`) before it's routed anywhere.
3. **Public routes** (no token required): `/api/auth/register`, `/api/auth/login`, `/actuator/**`. Configured via `app.security.public-paths` in `config-repo/api-gateway.properties` — not hardcoded in Java, so adding a new public route (e.g. catalog browsing in Phase 3) is a config change, not a redeploy.
4. Everything else requires a valid `Authorization: Bearer <token>` header. Missing, malformed, expired, or tampered tokens get a `401` from the gateway before the request reaches any business service.
5. On a valid token, the gateway injects `X-User-Id` and `X-User-Role` headers onto the forwarded request. **Downstream services read these headers and do not re-validate the JWT themselves** — they trust the gateway. (Business services besides auth don't exist yet, so this contract is proven by the gateway's own filter tests today; catalog/order/rating will be the first real consumers of `X-User-Id`/`X-User-Role` in later phases.)
6. The gateway also injects `X-Original-Path`, set unconditionally on every request — public or authenticated — to the path the client actually called (e.g. `/api/auth/login`), captured before the route's `StripPrefix=1` rewrites it for the downstream service (which sees `/auth/login`). Downstream error handlers should use this header (falling back to the local request path when it's absent) so error responses report the path the client called, not the internal one.

The signing secret (`app.jwt.secret`) lives in the **shared** `config-repo/application.properties`, not per-service — both `auth-service` (issuing) and `api-gateway` (verifying) read the same value. Local dev only; moves to an environment variable / Kubernetes `Secret` in Phase 6.

## Local database

`auth-service` (and every business service after it) needs a MySQL-compatible server. Two ways to get one, pick either:

- **Your own local MySQL/MariaDB instance.** Point `config-repo/<service>.properties` at it. If it's shared with other projects, prefix schema names with `cake_delight_` (e.g. `cake_delight_auth_db`) to avoid colliding with unrelated databases on the same instance — this project's own dev setup does exactly that, on a shared instance where a same-named `auth_db` already existed from other coursework.
- **The bundled `docker-compose.yml`** — `docker-compose up -d` starts a throwaway MySQL 8 container on host port `3307` (not `3306`/`3309`, to avoid clashing with anything else already running locally) and creates all five `cake_delight_<service>_db` schemas via `docker/mysql-init/`. If you use this, update the datasource URLs in `config-repo/` to point at `3307` instead of whatever the committed dev config uses.

Either way, `spring.jpa.hibernate.ddl-auto=update` means each service creates/updates its own tables on startup — no manual schema setup beyond the empty database existing.

## Configuration

All runtime config is centralized in `config-repo/`, served by `config-server`. Each service's own `src/main/resources/application.properties` holds only bootstrap essentials (app name, config-server URL) — everything else (ports, datasource, secrets, routes) lives in `config-repo/<service-name>.properties`. `config-server` and `eureka-server` are exceptions: they're foundational infrastructure that starts before anything else could usefully configure them, so their local `application.properties` holds their real settings directly (this is deliberate, not an oversight).

Config format is `.properties`, not YAML, project-wide.

Values that multiple services need identically (Eureka URL, actuator exposure, the JWT signing secret) live in the **shared** `config-repo/application.properties`, not duplicated per service — see Authentication above for why that matters for the JWT secret specifically.

**Secrets:** for local development, plaintext values (DB passwords, JWT secret) live directly in `config-repo/*.properties` — acceptable at this scope but never committed for anything beyond local dev. From Phase 6 onward, these move to environment variables locally and Kubernetes `Secret` resources in-cluster.

## Tech stack

Java 17, Spring Boot 4.1.x, Spring Cloud 2025.1.x, Maven, MySQL, Kafka, Eureka, Spring Cloud Gateway (Server WebMVC variant), JWT (HS256, `jjwt`), BCrypt (`spring-security-crypto`, not the full Spring Security starter), OpenFeign, Next.js. Full rationale and version history in `CLAUDE.md` (local dev notes, not committed).
