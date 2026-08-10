# Cake Delight

A cloud-native cake e-commerce app built as 8 Spring Boot microservices + a Next.js frontend. Capstone project — see `docs/audits/` for progress audits against the requirements.

**Status:** Phase 3 verified live end-to-end — `config-server`, `eureka-server`, `api-gateway`, `auth-service`, `catalog-service`, and `rating-service` all start cleanly and register with Eureka. Catalog browsing (`GET /api/catalog/cakes`, with filters, and `GET /api/catalog/cakes/{id}`) is public through the gateway; creating/updating/deleting a cake requires a token and the `ADMIN` role, enforced in `catalog-service` itself — confirmed live (403 for a customer token, 201/200/204 for an admin token). Every `/api/ratings/**` route requires a token, admin or not — submit, list, and summary all confirmed live, including the 401 (no token) and 409 (duplicate rating) cases. Remaining business services (`order-service`, `notification-service`) and the frontend are not started yet.

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
| `catalog-service` | 8082 | Cake catalog — list/filter/detail (public), create/update/delete (admin only) |
| `rating-service` | 8084 | Cake ratings — submit, list by cake, average/count summary (all require auth) |
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
- Eureka dashboard: `http://localhost:8761` — should show `AUTH-SERVICE`, `CATALOG-SERVICE`, `RATING-SERVICE`, and `API-GATEWAY` registered once all four are up.
- Register: `POST http://localhost:9090/api/auth/register` with `{"email": "...", "password": "..."}` (password ≥ 8 chars) — returns `201` and a JWT.
- Login: `POST http://localhost:9090/api/auth/login` with the same body — returns `200` and a JWT.
- Browse cakes (no token needed): `GET http://localhost:9090/api/catalog/cakes` or with filters, e.g. `GET http://localhost:9090/api/catalog/cakes?category=chocolate&maxPrice=500`. Seeded with ~14 sample cakes on startup (`catalog-service/src/main/resources/data.sql`).
- Manage cakes (token + `ADMIN` role required): `POST` / `PUT /api/catalog/cakes/{id}` / `DELETE /api/catalog/cakes/{id}`. A non-admin token gets `403`; no token gets `401`.
- `catalog-service` Swagger UI: `http://localhost:8082/swagger-ui.html` (direct, not yet proxied through the gateway).
- Submit a rating (token required, any authenticated user): `POST http://localhost:9090/api/ratings` with `{"cakeId": 1, "ratingValue": 5, "reviewText": "..."}` — returns `201`. Rating the same cake twice as the same user returns `409`.
- Ratings for a cake / summary (token required): `GET http://localhost:9090/api/ratings/cakes/{cakeId}` and `GET http://localhost:9090/api/ratings/cakes/{cakeId}/summary` — the latter returns `{averageRating, totalRatings}`.
- `rating-service` Swagger UI: `http://localhost:8084/swagger-ui.html` (direct, not yet proxied through the gateway).
- Anything else through the gateway needs `Authorization: Bearer <token>` — a missing or invalid token gets a `401` in the standard error shape (see `api-conventions` skill).

## Authentication & identity propagation

The gateway is the trust boundary — it's the only service that validates the JWT; everything downstream trusts what the gateway tells it.

1. `auth-service` issues an HS256 JWT on register/login, containing `sub` (user id), `email`, `role`, `iss`, `iat`, `exp`.
2. Every request through `api-gateway` passes through a servlet filter (`JwtAuthenticationFilter`) before it's routed anywhere.
3. **Public routes** come in two flavors, both config-driven via `config-repo/api-gateway.properties` — not hardcoded in Java, so adding one is a config change, not a redeploy:
   - `app.security.public-paths` — public regardless of HTTP method: `/api/auth/register`, `/api/auth/login`, plus `/actuator/**` (always public, not config-driven).
   - `app.security.public-get-paths` — public for `GET` only: `/api/catalog/cakes/**`. `POST`/`PUT`/`DELETE` on the same path still need a token — this is what lets catalog browsing be open while admin mutations stay gated. Both lists support Ant-style patterns (`/**`), matched with `AntPathMatcher`.
4. Everything else requires a valid `Authorization: Bearer <token>` header. Missing, malformed, expired, or tampered tokens get a `401` from the gateway before the request reaches any business service.
5. On a valid token, the gateway injects `X-User-Id` and `X-User-Role` headers onto the forwarded request. **Downstream services read these headers and do not re-validate the JWT themselves** — they trust the gateway. The gateway only proves *who* the caller is; *what they're allowed to do* is enforced downstream — `catalog-service` is the first real consumer of this, checking `X-User-Role == ADMIN` in its service layer before create/update/delete.
6. The gateway also injects `X-Original-Path`, set unconditionally on every request — public or authenticated — to the path the client actually called (e.g. `/api/auth/login`), captured before the route's `StripPrefix=1` rewrites it for the downstream service (which sees `/auth/login`). Downstream error handlers should use this header (falling back to the local request path when it's absent) so error responses report the path the client called, not the internal one.

The signing secret (`app.jwt.secret`) lives in the **shared** `config-repo/application.properties`, not per-service — both `auth-service` (issuing) and `api-gateway` (verifying) read the same value. Local dev only; moves to an environment variable / Kubernetes `Secret` in Phase 6.

## Local database

`auth-service` (and every business service after it) needs a MySQL-compatible server. Two ways to get one, pick either:

- **Your own local MySQL/MariaDB instance.** Point `config-repo/<service>.properties` at it. If it's shared with other projects, prefix schema names with `cake_delight_` (e.g. `cake_delight_auth_db`) to avoid colliding with unrelated databases on the same instance — this project's own dev setup does exactly that, on a shared instance where a same-named `auth_db` already existed from other coursework. **The schemas are not created for you here** — `docker/mysql-init/` only runs against the docker-compose container below, so on your own instance you need to run its `CREATE DATABASE` statements yourself (e.g. via `mysql -h 127.0.0.1 -P <port> -u root < docker/mysql-init/001-init-schemas.sql`, or the equivalent in a GUI client) before starting a service that needs a schema that isn't there yet — Hibernate creates *tables*, never the database itself.
- **The bundled `docker-compose.yml`** — `docker-compose up -d` starts a throwaway MySQL 8 container on host port `3307` (not `3306`/`3309`, to avoid clashing with anything else already running locally) and creates all five `cake_delight_<service>_db` schemas via `docker/mysql-init/`. If you use this, update the datasource URLs in `config-repo/` to point at `3307` instead of whatever the committed dev config uses.

Either way, `spring.jpa.hibernate.ddl-auto=update` means each service creates/updates its own *tables* on startup once its schema exists.

## Configuration

All runtime config is centralized in `config-repo/`, served by `config-server`. Each service's own `src/main/resources/application.properties` holds only bootstrap essentials (app name, config-server URL) — everything else (ports, datasource, secrets, routes) lives in `config-repo/<service-name>.properties`. `config-server` and `eureka-server` are exceptions: they're foundational infrastructure that starts before anything else could usefully configure them, so their local `application.properties` holds their real settings directly (this is deliberate, not an oversight).

Config format is `.properties`, not YAML, project-wide.

Values that multiple services need identically (Eureka URL, actuator exposure, the JWT signing secret) live in the **shared** `config-repo/application.properties`, not duplicated per service — see Authentication above for why that matters for the JWT secret specifically.

**Secrets:** for local development, plaintext values (DB passwords, JWT secret) live directly in `config-repo/*.properties` — acceptable at this scope but never committed for anything beyond local dev. From Phase 6 onward, these move to environment variables locally and Kubernetes `Secret` resources in-cluster.

## Phase 3 manual smoke test

Run through this once all six services are up (`config-server`, `eureka-server`, `auth-service`, `catalog-service`, `rating-service`, `api-gateway`) to confirm Phase 3 end-to-end, all through the gateway on `:9090`.

**Before you start:** registration always creates a `CUSTOMER` (`AuthServiceImpl` hardcodes the role — there's no self-serve admin signup). To test the admin-only catalog endpoints, register normally, then manually set `role='ADMIN'` on that row in `cake_delight_auth_db.users` and log in again to get a fresh token with the new role — the old token still carries the stale `CUSTOMER` claim until it expires.

1. Register two users (one you'll promote to admin, one that stays a customer); log in as each and keep both tokens.
2. `GET /api/catalog/cakes` with no token — expect `200` and ~14 seeded cakes.
3. `GET /api/catalog/cakes?category=chocolate&maxPrice=550` — expect a filtered subset.
4. `POST /api/catalog/cakes` as the customer token — expect `403`.
5. `POST /api/catalog/cakes` as the admin token — expect `201`; note the new cake's `id`.
6. `PUT` / `DELETE /api/catalog/cakes/{id}` on that cake as admin — expect `200` / `204`.
7. `POST /api/ratings` with no token — expect `401`.
8. `POST /api/ratings` as the customer token, rating a seeded cake `{cakeId, ratingValue: 5}` — expect `201`.
9. Repeat step 8 for the same cake + same user — expect `409`.
10. `GET /api/ratings/cakes/{cakeId}/summary` — expect `{averageRating: 5.0, totalRatings: 1}`.
11. Eureka dashboard shows all four business services + gateway registered; both services' Swagger UIs load directly on their own ports.

## Tech stack

Java 17, Spring Boot 4.1.x, Spring Cloud 2025.1.x, Maven, MySQL, Kafka, Eureka, Spring Cloud Gateway (Server WebMVC variant), JWT (HS256, `jjwt`), BCrypt (`spring-security-crypto`, not the full Spring Security starter), OpenFeign, Next.js. Full rationale and version history in `CLAUDE.md` (local dev notes, not committed).
