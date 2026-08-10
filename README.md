# Cake Delight

A cloud-native cake e-commerce app built as 8 Spring Boot microservices + a Next.js frontend. Capstone project — see `docs/audits/` for progress audits against the requirements.

**Status:** Phase 4 (`order-service`, `notification-service`, Kafka) built and unit/slice-tested; full live smoke test against running services is still pending (see the Phase 4 checklist below). Through Phase 3: `config-server`, `eureka-server`, `api-gateway`, `auth-service`, `catalog-service`, and `rating-service` all start cleanly and register with Eureka. Catalog browsing (`GET /api/catalog/cakes`, with filters, and `GET /api/catalog/cakes/{id}`) is public through the gateway; creating/updating/deleting a cake requires a token and the `ADMIN` role, enforced in `catalog-service` itself — confirmed live (403 for a customer token, 201/200/204 for an admin token). Every `/api/ratings/**` route requires a token, admin or not — submit, list, and summary all confirmed live, including the 401 (no token) and 409 (duplicate rating) cases. `order-service` now handles basket/checkout and `notification-service` consumes `order.completed` off Kafka; ratings additionally require the caller to have actually purchased the cake (`order-service` backs this via an internal Feign call). The frontend is not started yet.

## Prerequisites

- JDK 17
- Maven 3.9+ (each service also ships its own `mvnw` wrapper)
- A MySQL-compatible server reachable locally (see **Local database** below) — needed from `auth-service` onward
- Docker Desktop (for the bundled MySQL via `docker-compose`, and — from Phase 4 — Kafka + Zookeeper, needed by `order-service` and `notification-service`)
- Node.js 20+ (for the frontend, from Phase 5 onward)
- IntelliJ IDEA (or any IDE with Spring Boot support)

## Services and ports

| Service | Port | Purpose |
|---|---|---|
| `config-server` | 8888 | Centralized config, serves everything from `config-repo/` |
| `eureka-server` | 8761 | Service registry — dashboard at `http://localhost:8761` |
| `auth-service` | 8081 | Registration, login, JWT issuance |
| `catalog-service` | 8082 | Cake catalog — list/filter/detail (public), create/update/delete (admin only) |
| `order-service` | 8083 | Basket CRUD, checkout, order history — publishes `order.completed` to Kafka (all require auth) |
| `rating-service` | 8084 | Cake ratings — submit (purchase-verified), list by cake, average/count summary (all require auth) |
| `notification-service` | 8085 | Consumes `order.completed`, records a notification per order (all require auth) |
| `api-gateway` | **9090** | Single public entry point |

> **Note:** `api-gateway` runs on `9090`, not the more conventional `8080`. This is a local port conflict (XAMPP occupies `8080` on the primary dev machine), not a design decision — if your environment doesn't have that conflict, the port is still `9090` per `config-repo/api-gateway.properties`; change it there if you'd rather use `8080`.

## Startup order

Services must start in this order — each one depends on the previous being up:

```
1. docker-compose up -d         (MySQL + Kafka/Zookeeper — see Local database and Eventing below)
2. config-server                (mvn spring-boot:run, or run the main class in your IDE)
3. eureka-server
4. auth-service, catalog-service, order-service, rating-service, notification-service
                                 (any order among themselves — Kafka buffers events, so order-service
                                  and notification-service don't need to start in a particular order
                                  relative to each other)
5. api-gateway                  (last, so it can resolve lb:// routes to already-registered services)
```

Wait for each service to fully start (watch for `Started XxxApplication` in the console) before starting the next.

Once running:
- Config server: `http://localhost:8888/auth-service/default` (or any service name) — should return that service's resolved config as JSON.
- Eureka dashboard: `http://localhost:8761` — should show `AUTH-SERVICE`, `CATALOG-SERVICE`, `ORDER-SERVICE`, `RATING-SERVICE`, `NOTIFICATION-SERVICE`, and `API-GATEWAY` registered once all six are up.
- Register: `POST http://localhost:9090/api/auth/register` with `{"email": "...", "password": "..."}` (password ≥ 8 chars) — returns `201` and a JWT.
- Login: `POST http://localhost:9090/api/auth/login` with the same body — returns `200` and a JWT.
- Browse cakes (no token needed): `GET http://localhost:9090/api/catalog/cakes` or with filters, e.g. `GET http://localhost:9090/api/catalog/cakes?category=chocolate&maxPrice=500`. Seeded with ~14 sample cakes on startup (`catalog-service/src/main/resources/data.sql`).
- Manage cakes (token + `ADMIN` role required): `POST` / `PUT /api/catalog/cakes/{id}` / `DELETE /api/catalog/cakes/{id}`. A non-admin token gets `403`; no token gets `401`.
- `catalog-service` Swagger UI: `http://localhost:8082/swagger-ui.html` (direct, not yet proxied through the gateway).
- Basket (token required): `GET http://localhost:9090/api/orders/basket` (auto-creates an empty basket on first call), `POST .../api/orders/basket/items` with `{"cakeId": 1, "quantity": 2}` (`404` if the cake doesn't exist, `409` if it's unavailable), `PUT .../api/orders/basket/items/{itemId}` with `{"quantity": 3}`, `DELETE .../api/orders/basket/items/{itemId}` (`204`).
- Checkout (token required): `POST http://localhost:9090/api/orders/checkout` — `400` on an empty basket, otherwise `201` with the created order; clears the basket and publishes `order.completed` to Kafka.
- Orders (token required): `GET http://localhost:9090/api/orders` (current user's orders) and `GET http://localhost:9090/api/orders/{id}` (`404` if it's not this user's order).
- `order-service` Swagger UI: `http://localhost:8083/swagger-ui.html` (direct).
- Submit a rating (token required, and the caller must have purchased the cake — `403` otherwise): `POST http://localhost:9090/api/ratings` with `{"cakeId": 1, "ratingValue": 5, "reviewText": "..."}` — returns `201`. Rating the same cake twice as the same user returns `409`.
- Ratings for a cake / summary (token required): `GET http://localhost:9090/api/ratings/cakes/{cakeId}` and `GET http://localhost:9090/api/ratings/cakes/{cakeId}/summary` — the latter returns `{averageRating, totalRatings}`.
- `rating-service` Swagger UI: `http://localhost:8084/swagger-ui.html` (direct, not yet proxied through the gateway).
- Notifications (token required): `GET http://localhost:9090/api/notifications` — lists the current user's notifications, one row per completed order once `notification-service` has consumed the event (see **Eventing (Kafka)** below).
- `notification-service` Swagger UI: `http://localhost:8085/swagger-ui.html` (direct).
- Anything else through the gateway needs `Authorization: Bearer <token>` — a missing or invalid token gets a `401` in the standard error shape (see `api-conventions` skill).

## Authentication & identity propagation

The gateway is the trust boundary — it's the only service that validates the JWT; everything downstream trusts what the gateway tells it.

1. `auth-service` issues an HS256 JWT on register/login, containing `sub` (user id), `email`, `role`, `iss`, `iat`, `exp`.
2. Every request through `api-gateway` passes through a servlet filter (`JwtAuthenticationFilter`) before it's routed anywhere.
3. **Public routes** come in two flavors, both config-driven via `config-repo/api-gateway.properties` — not hardcoded in Java, so adding one is a config change, not a redeploy:
   - `app.security.public-paths` — public regardless of HTTP method: `/api/auth/register`, `/api/auth/login`, plus `/actuator/**` (always public, not config-driven).
   - `app.security.public-get-paths` — public for `GET` only: `/api/catalog/cakes/**`. `POST`/`PUT`/`DELETE` on the same path still need a token — this is what lets catalog browsing be open while admin mutations stay gated. Both lists support Ant-style patterns (`/**`), matched with `AntPathMatcher`.
4. Everything else requires a valid `Authorization: Bearer <token>` header. Missing, malformed, expired, or tampered tokens get a `401` from the gateway before the request reaches any business service.
5. On a valid token, the gateway injects `X-User-Id`, `X-User-Role`, and (Phase 4) `X-User-Email` headers onto the forwarded request. **Downstream services read these headers and do not re-validate the JWT themselves** — they trust the gateway. The gateway only proves *who* the caller is; *what they're allowed to do* is enforced downstream — `catalog-service` is the first real consumer of this, checking `X-User-Role == ADMIN` in its service layer before create/update/delete. `X-User-Email` exists solely so `order-service` can embed it in the `order.completed` event payload at checkout — it's optional (absent if the JWT's `email` claim is missing) and nothing else reads it.
6. The gateway also injects `X-Original-Path`, set unconditionally on every request — public or authenticated — to the path the client actually called (e.g. `/api/auth/login`), captured before the route's `StripPrefix=1` rewrites it for the downstream service (which sees `/auth/login`). Downstream error handlers should use this header (falling back to the local request path when it's absent) so error responses report the path the client called, not the internal one.

The signing secret (`app.jwt.secret`) lives in the **shared** `config-repo/application.properties`, not per-service — both `auth-service` (issuing) and `api-gateway` (verifying) read the same value. Local dev only; moves to an environment variable / Kubernetes `Secret` in Phase 6.

### Internal (service-to-service) endpoints

Phase 4 adds `order-service`'s first Feign-only endpoint: `GET /internal/orders/purchases?userId=&cakeId=`, called by `rating-service` to enforce "only users who purchased the cake can rate it" (CLAUDE.md §5.2). Two things make it different from every other endpoint in the system:

- It's resolved via Eureka (`lb://order-service`) directly, **never through the gateway** — same as coding-guidelines §8 describes for all Feign calls.
- It's deliberately mounted at `/internal/orders/**`, not under `/orders/**`. The gateway's existing route (`Path=/api/orders/**`) would otherwise proxy it too, and since this endpoint takes `userId` as a plain query parameter rather than trusting the gateway-injected `X-User-Id` header, that would let any authenticated end user query whether an *arbitrary other* user purchased a cake — a real authorization leak. There's no gateway route for `/api/internal/**`, so the path 404s at the gateway instead of needing extra deny-list logic to stay unreachable from outside.

This is the first "internal-only" endpoint in the project; the same `/internal/**` convention should be reused if a later phase needs another one, rather than inventing a new pattern per service.

## Local database

`auth-service` (and every business service after it) needs a MySQL-compatible server. Two ways to get one, pick either:

- **Your own local MySQL/MariaDB instance.** Point `config-repo/<service>.properties` at it. If it's shared with other projects, prefix schema names with `cake_delight_` (e.g. `cake_delight_auth_db`) to avoid colliding with unrelated databases on the same instance — this project's own dev setup does exactly that, on a shared instance where a same-named `auth_db` already existed from other coursework. **The schemas are not created for you here** — `docker/mysql-init/` only runs against the docker-compose container below, so on your own instance you need to run its `CREATE DATABASE` statements yourself (e.g. via `mysql -h 127.0.0.1 -P <port> -u root < docker/mysql-init/001-init-schemas.sql`, or the equivalent in a GUI client) before starting a service that needs a schema that isn't there yet — Hibernate creates *tables*, never the database itself.
- **The bundled `docker-compose.yml`** — `docker-compose up -d` starts a throwaway MySQL 8 container on host port `3307` (not `3306`/`3309`, to avoid clashing with anything else already running locally) and creates all five `cake_delight_<service>_db` schemas via `docker/mysql-init/`. If you use this, update the datasource URLs in `config-repo/` to point at `3307` instead of whatever the committed dev config uses.

Either way, `spring.jpa.hibernate.ddl-auto=update` means each service creates/updates its own *tables* on startup once its schema exists.

## Eventing (Kafka)

Phase 4 adds a single Kafka topic, `order.completed`, per CLAUDE.md §5.3 — `order-service` produces it on checkout, `notification-service` consumes it. Full contract (payload field types, key, versioning note): `docs/event-contract.md`.

- **Local broker:** `docker-compose.yml` runs `zookeeper` + `kafka` (single broker, `confluentinc/cp-kafka`) alongside MySQL. `docker-compose up -d` starts all of it. The broker is reachable at `localhost:9092` from host-run services and at `kafka:29092` from other containers (not used yet — every service runs on the host until Phase 6).
- **Producer** (`order-service`): publishes on a successful checkout, after the order is saved and the basket is cleared, keyed by `orderId`. No transactional outbox — a checkout that fails between the DB commit and the Kafka send is an accepted, documented gap for this capstone's scope (CLAUDE.md §3 rules out Saga/event-sourcing machinery), not an oversight.
- **Consumer** (`notification-service`): `@KafkaListener` on the topic, idempotent by `eventId` — a redelivered message (Kafka is at-least-once) is detected via a unique `event_id` column and skipped rather than double-recorded.
- **Payload:** exact shape in CLAUDE.md §5.3 — `eventId`, `eventType`, `occurredAt`, `orderId`, `userId`, `userEmail`, `totalAmount`, `items[]`. Self-contained by design: `notification-service` never calls back to `order-service`/`catalog-service`/`auth-service` for anything it needs. Both sides keep their own copy of the record (`OrderCompletedEvent`) rather than sharing a JAR (CLAUDE.md §10).
- **Config:** `spring.kafka.bootstrap-servers` and the topic name (`app.kafka.topics.order-completed`) live in the shared `config-repo/application.properties` since both services need the identical values; producer/consumer-specific settings (serializers, consumer group id) stay in each service's own `config-repo/<service>.properties`.

## Real email notifications

`notification-service` sends a real order-confirmation email over SMTP (Gmail, App Password auth) by default — the `LoggingNotificationSender` console-only behavior from earlier in Phase 4 is now an opt-in fallback, not the default. Which one runs is picked by `NotificationSender.channel()`/`app.notification.channel`; see `EmailNotificationSender`/`LoggingNotificationSender` in `notification-service`.

**One-time account setup** (see `docs/audits` chat history for the full walkthrough — summarized here):
1. Create a dedicated Gmail account for sending (this project uses `cakedelight.donotreply@gmail.com`) — don't reuse a personal account.
2. Turn on **2-Step Verification** on that account (`myaccount.google.com/security`) — required before Gmail will issue an App Password.
3. Generate an **App Password** (same page, search "App passwords") — a 16-character string, shown once. This, not the account's regular password, is what SMTP auth uses.

**Local config — where the app password goes:**
- `config-repo/notification-service.properties` holds `spring.mail.host`, `port`, `username` (the sender address — not a secret, committed as a literal default), and `spring.mail.password=${SMTP_PASSWORD:}` — the app password is **never** written into this file or committed. It's read from the `SMTP_PASSWORD` environment variable at runtime.
- Set it before running `notification-service`:
  - **IntelliJ:** Run/Debug Configurations → `NotificationServiceApplication` → Modify options → Environment variables → add `SMTP_PASSWORD=<the 16-character app password, spaces removed>`.
  - **Shell (this session):** `$env:SMTP_PASSWORD = "xxxxxxxxxxxxxxxx"` (PowerShell) before starting the service in that same terminal — session-scoped, not persisted.
- `SMTP_USERNAME` and `NOTIFICATION_CHANNEL` are optional overrides of the same shape, if the sending address or channel ever needs to change without editing `config-repo/`.

**Falling back to log-only:** set `NOTIFICATION_CHANNEL=log` (or edit `app.notification.channel` in `config-repo/notification-service.properties`) to route through `LoggingNotificationSender` instead — no SMTP credentials needed, notifications are console-only and stored with `channel: "IN_APP"`.

**Failure behavior:** if `SMTP_PASSWORD` is unset, wrong, or the SMTP host is unreachable, `EmailNotificationSender.send()` throws, and `NotificationServiceImpl` records the notification as `status: "FAILED"` (still stored, not silently dropped) rather than retrying — see `docs/db-schema.md`'s `notifications` table.

## Configuration

All runtime config is centralized in `config-repo/`, served by `config-server`. Each service's own `src/main/resources/application.properties` holds only bootstrap essentials (app name, config-server URL) — everything else (ports, datasource, secrets, routes) lives in `config-repo/<service-name>.properties`. `config-server` and `eureka-server` are exceptions: they're foundational infrastructure that starts before anything else could usefully configure them, so their local `application.properties` holds their real settings directly (this is deliberate, not an oversight).

Config format is `.properties`, not YAML, project-wide.

Values that multiple services need identically (Eureka URL, actuator exposure, the JWT signing secret) live in the **shared** `config-repo/application.properties`, not duplicated per service — see Authentication above for why that matters for the JWT secret specifically.

**Secrets:** for local development, plaintext values (DB passwords, JWT secret) live directly in `config-repo/*.properties` — acceptable at this scope but never committed for anything beyond local dev. From Phase 6 onward, these move to environment variables locally and Kubernetes `Secret` resources in-cluster.

## Phase 3 & 4 manual smoke test

Run through this once all eight services are up (`config-server`, `eureka-server`, `auth-service`, `catalog-service`, `order-service`, `rating-service`, `notification-service`, `api-gateway`, plus `docker-compose up -d` for MySQL + Kafka) to confirm Phase 3 and Phase 4 end-to-end, all through the gateway on `:9090`. Steps 1–11 are the Phase 3 test, unchanged; 12+ are new for Phase 4 — note step 7 now needs a purchase first, which is why the rating steps moved after checkout.

**Before you start:** registration always creates a `CUSTOMER` (`AuthServiceImpl` hardcodes the role — there's no self-serve admin signup). To test the admin-only catalog endpoints, register normally, then manually set `role='ADMIN'` on that row in `cake_delight_auth_db.users` and log in again to get a fresh token with the new role — the old token still carries the stale `CUSTOMER` claim until it expires.

1. Register two users (one you'll promote to admin, one that stays a customer); log in as each and keep both tokens.
2. `GET /api/catalog/cakes` with no token — expect `200` and ~14 seeded cakes.
3. `GET /api/catalog/cakes?category=chocolate&maxPrice=550` — expect a filtered subset.
4. `POST /api/catalog/cakes` as the customer token — expect `403`.
5. `POST /api/catalog/cakes` as the admin token — expect `201`; note the new cake's `id`.
6. `PUT` / `DELETE /api/catalog/cakes/{id}` on that cake as admin — expect `200` / `204`.
7. `POST /api/ratings` as the customer token, rating a seeded cake **without ever having ordered it** `{cakeId, ratingValue: 5}` — expect `403` (`CAKE_NOT_PURCHASED`). This is the new Phase 4 check; do the actual rating in step 16, after checkout.
8. `GET /api/orders/basket` as the customer token — expect `200` and an empty basket (auto-created).
9. `POST /api/orders/basket/items` with `{"cakeId": <a seeded id>, "quantity": 2}` — expect `201` and the basket now has one line. Repeat with the same `cakeId` — expect the same line's quantity to increase to `4`, not a second line.
10. `PUT /api/orders/basket/items/{itemId}` with `{"quantity": 1}` — expect `200` and the updated quantity.
11. `POST /api/orders/checkout` — expect `201`, an order with the right total, and the basket now empty (`GET /api/orders/basket` confirms).
12. `GET /api/orders` and `GET /api/orders/{id}` — expect the order from step 11 in both.
13. Watch `notification-service`'s console — expect an `Email notification sent for order ... to ...` log line shortly after checkout (Kafka delivery isn't instant, but should be well under a second locally), and a real email arriving in the checkout user's inbox. If `SMTP_PASSWORD` isn't set locally, expect a `Failed to send notification for order ...` error log instead — see **Real email notifications** above; that's an expected result of missing credentials, not a bug.
14. `GET /api/notifications` as the same user — expect one row, `channel: "EMAIL"`, `status: "SENT"` (or `"FAILED"` if SMTP credentials aren't configured locally — see step 13).
15. Check `cake_delight_notification_db.notifications` directly — one row, `event_id` matching the order's checkout.
16. `POST /api/ratings` again for the cake purchased in step 9/11 — expect `201` this time.
17. Repeat step 16 for the same cake + same user — expect `409`.
18. `GET /api/ratings/cakes/{cakeId}/summary` — expect `{averageRating: 5.0, totalRatings: 1}`.
19. Eureka dashboard shows all six business services + gateway registered; every service's Swagger UI loads directly on its own port.

## Tech stack

Java 17, Spring Boot 4.1.x, Spring Cloud 2025.1.x, Maven, MySQL, Kafka, Eureka, Spring Cloud Gateway (Server WebMVC variant), JWT (HS256, `jjwt`), BCrypt (`spring-security-crypto`, not the full Spring Security starter), OpenFeign, Next.js. Full rationale and version history in `CLAUDE.md` (local dev notes, not committed).
