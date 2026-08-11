# Cake Delight

A cloud-native cake e-commerce app built as 8 Spring Boot microservices + a Next.js frontend. Capstone project — see `docs/audits/` for progress audits against the requirements.

**Status:** Phases 1–6 complete. Backend (1–4), frontend (5), and containerization + Kubernetes (6) all built and tested live end-to-end — the full flow (register → log in → browse/filter cakes → add to basket → update quantity → checkout → Kafka → notification recorded → rate a purchased cake → average updates) has been driven through the gateway in three environments: services running directly on the host, the full stack under Docker Compose, and the full stack deployed to Kubernetes (Docker Desktop). See **Frontend (`frontend-service`)** below for the local/IDE walkthrough, and **Phase 6 — Docker & Kubernetes** for the containerized ones.

## Prerequisites

- JDK 17
- Maven 3.9+ (each service also ships its own `mvnw` wrapper)
- A MySQL-compatible server reachable locally (see **Local database** below) — needed from `auth-service` onward
- Docker Desktop (for the bundled MySQL via `docker-compose`, and — from Phase 4 — Kafka + Zookeeper, needed by `order-service` and `notification-service`; from Phase 6, also for running the whole stack containerized and its built-in Kubernetes — see **Phase 6 — Docker & Kubernetes** below)
- Node.js 20+ (for `frontend-service`)
- IntelliJ IDEA (or any IDE with Spring Boot support) — only needed for the host/IDE-based workflow described in this section; Phase 6's Docker/Kubernetes workflow needs only Docker Desktop and `kubectl`

**Note:** everything above **this line** describes running each service directly on the host (via IDE or `mvn spring-boot:run`) — Phases 1–5's original workflow, still fully supported. For running the whole stack containerized (Docker Compose) or in Kubernetes, skip to **Phase 6 — Docker & Kubernetes** near the end of this file — it needs only Docker Desktop, not a JDK/Maven/Node install on the host.

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
| `frontend-service` | 3000 | Next.js app (`npm run dev`) — talks to `api-gateway`, not any service directly |

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
6. frontend-service             (npm run dev — see Frontend below; optional, only needed to use the UI)
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

## Frontend (`frontend-service`)

Next.js 16 (App Router, TypeScript), Tailwind CSS v4, shadcn/ui. Talks to the backend exclusively through `api-gateway` on `:9090` — never a business service directly, same trust-boundary rule as everything else in CLAUDE.md §4.

- **Run it:** `cd frontend-service && npm install && npm run dev` — serves on `http://localhost:3000`. Needs the full backend stack up first (see **Startup order** above); the catalog list/detail pages work without login, everything else (basket, checkout, orders, ratings, notifications) needs a registered/logged-in user.
- **Config:** `NEXT_PUBLIC_API_BASE_URL` (see `.env.local.example`) — defaults to `http://localhost:9090/api` if unset.
- **Session:** the JWT + `{email, role}` from login/register live in `localStorage` (`lib/auth-storage.ts`), attached as `Authorization: Bearer <token>` on every request by `lib/api-client.ts`. A `401` on an authenticated request clears the session and redirects to `/login`.
- **CORS:** `api-gateway` didn't have any CORS configuration before Phase 5 — none of the 8 services did, since nothing had called the gateway from a browser yet. `CorsConfig` (`api-gateway/src/main/java/.../config/CorsConfig.java`) now allows `app.frontend.origin` (`config-repo/api-gateway.properties`, defaults to `http://localhost:3000`). `JwtAuthenticationFilter` also had to let `OPTIONS` (CORS preflight) through unconditionally, ahead of the token check — browsers never attach `Authorization` to a preflight, so the filter was 401-ing every preflight for a protected route and silently blocking the real request behind it. Caught live in the first browser smoke test; see the filter's own comment for the full explanation.
- **No admin UI.** CLAUDE.md §12 rules out an admin dashboard as scope creep — cake CRUD stays backend-only (`data.sql` seeding + direct API calls), the frontend only consumes the customer-facing endpoints (catalog browse, basket, checkout, orders, ratings, notifications).
- **Ratings require login for the GETs too** — unlike catalog, `api-gateway`'s `app.security.public-get-paths` has no entry for `/api/ratings/**`, so the cake detail page only fetches/shows ratings once a user is signed in.

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

## Phase 6 — Docker & Kubernetes

Every service (the 8 Spring services + `frontend-service`) has its own multi-stage `Dockerfile` (build stage brings its own Maven/Node — no host build required) and `.dockerignore`. `docker-compose.yml` runs the whole stack containerized; `k8s/` deploys the same images to Kubernetes (built and tested against **Docker Desktop's built-in Kubernetes**). Both targets read the exact same `config-repo/` and the exact same `${VAR:default}` environment-variable pattern already used for `JWT_SECRET`/`SMTP_PASSWORD` (see **Secrets** below) — extended to `CONFIG_SERVER_HOST`, `EUREKA_URL`, `DB_HOST`/`DB_PORT`, `KAFKA_BOOTSTRAP_SERVERS`, and `FRONTEND_ORIGIN` so the same code and config run unchanged on the host, in Compose, and in-cluster.

> **Windows + OneDrive note:** if this repo lives under a OneDrive-synced folder (as the author's dev machine does), Docker's default BuildKit builder fails with `ERROR: invalid file request <file>` — a known interaction between BuildKit's lazy file-transfer protocol and OneDrive's placeholder files. The legacy builder doesn't hit this. Every build command below sets `DOCKER_BUILDKIT=0` (and `COMPOSE_DOCKER_CLI_BUILD=0` for Compose) for that reason. If your clone isn't under OneDrive, these are harmless no-ops and can be dropped.

### Docker Compose

```sh
cp .env.example .env        # see "Secrets" below before editing this
DOCKER_BUILDKIT=0 COMPOSE_DOCKER_CLI_BUILD=0 docker compose build
DOCKER_BUILDKIT=0 COMPOSE_DOCKER_CLI_BUILD=0 docker compose up -d
```

MySQL, Kafka, and Zookeeper come up first (existing Phase 4 healthchecks), then `config-server` and `eureka-server`, then the five business services, then `api-gateway`, then `frontend-service` — `depends_on`/`condition: service_healthy` in `docker-compose.yml` enforces this order automatically; no manual sequencing needed, unlike running services individually on the host. Once everything is `Up (healthy)` (`docker compose ps`), the app is reachable at the same URLs as local dev: gateway on `http://localhost:9090`, frontend on `http://localhost:3000`. `docker compose logs -f <service>` for any single service's logs; `docker compose down` to stop (add `-v` to also drop the MySQL volume).

Each service's image is tagged `cake-delight/<service>:latest` (set via `image:` in `docker-compose.yml`) — the same tags the Kubernetes manifests reference, so `docker compose build` doubles as the image build step for Kubernetes too, with one exception (`frontend-service` — see below).

### Kubernetes (Docker Desktop)

Prerequisite: Docker Desktop's Kubernetes enabled (Settings → Kubernetes → Enable Kubernetes), `kubectl config current-context` reporting `docker-desktop`. No `minikube`/`kind` image-loading step is needed — Docker Desktop's Kubernetes shares the same image cache as `docker build`, so an image built on the host is immediately usable by the cluster (`imagePullPolicy: IfNotPresent` in every manifest is what makes this work, instead of trying to pull from a registry).

**If the Docker Compose stack is already running, stop it first** (`docker compose stop`) — both targets run on the same Docker Desktop VM and will otherwise compete for the same memory, which is exactly what caused repeated `OOMKilled` pods during this project's own Phase 6 deployment (see the troubleshooting note below).

```sh
# 1. Build the 8 Spring images (reuses docker-compose.yml's image: tags)
DOCKER_BUILDKIT=0 COMPOSE_DOCKER_CLI_BUILD=0 docker compose build

# 2. Rebuild frontend-service specifically for Kubernetes — NEXT_PUBLIC_API_BASE_URL
#    is baked in at build time (a Next.js constraint, not a Cake Delight choice — see
#    frontend-service/Dockerfile's comment), and the gateway's Kubernetes NodePort
#    (30090, below) differs from Compose's host port mapping (9090).
DOCKER_BUILDKIT=0 docker build \
  --build-arg NEXT_PUBLIC_API_BASE_URL=http://localhost:30090/api \
  -t cake-delight/frontend-service:latest ./frontend-service

# 3. Namespace, then the config-repo ConfigMap (generated, not committed — see
#    k8s/config/README.md for why) and the shared env ConfigMap
kubectl apply -f k8s/namespace.yaml
kubectl create configmap cake-delight-config-repo --from-file=config-repo/ -n cake-delight
kubectl apply -f k8s/config/env-configmap.yaml -n cake-delight

# 4. Secrets — see "Secrets" below before running this
kubectl create secret generic cake-delight-secrets \
  --from-literal=JWT_SECRET="$(openssl rand -base64 32)" \
  --from-literal=SMTP_PASSWORD="" \
  -n cake-delight

# 5. MySQL, then Kafka/Zookeeper
kubectl apply -f k8s/mysql/mysql.yaml -n cake-delight
kubectl apply -f k8s/kafka/kafka.yaml -n cake-delight

# 6. config-server and eureka-server — wait for both healthy before continuing
kubectl apply -f k8s/services/config-server.yaml -n cake-delight
kubectl apply -f k8s/services/eureka-server.yaml -n cake-delight
kubectl rollout status deployment/config-server -n cake-delight
kubectl rollout status deployment/eureka-server -n cake-delight

# 7. The five business services
kubectl apply -f k8s/services/auth-service.yaml -n cake-delight
kubectl apply -f k8s/services/catalog-service.yaml -n cake-delight
kubectl apply -f k8s/services/order-service.yaml -n cake-delight
kubectl apply -f k8s/services/rating-service.yaml -n cake-delight
kubectl apply -f k8s/services/notification-service.yaml -n cake-delight

# 8. api-gateway, then frontend-service
kubectl apply -f k8s/services/api-gateway.yaml -n cake-delight
kubectl apply -f k8s/services/frontend-service.yaml -n cake-delight
```

Verify: `kubectl get pods -n cake-delight` — everything `1/1 Running`. `catalog-service` runs at **2 replicas** (the rest at 1) to demonstrate statelessness — neither replica holds any in-memory state, matching the basket-in-a-database design from Phase 4. The app is then reachable at fixed NodePorts (chosen deliberately, not the random 30000–32767 Kubernetes would otherwise assign, so the URLs below are stable across redeploys): gateway at `http://localhost:30090/api/...`, frontend at `http://localhost:30300`. Only these two Services are `NodePort`; every other service (business services, MySQL, Kafka, Zookeeper, config-server, eureka-server) is `ClusterIP`-only — the same trust-boundary rule as local dev (nothing but the gateway is meant to be reachable from outside), now actually enforced by the cluster rather than just documented.

Teardown: `kubectl delete namespace cake-delight` (drops everything, including the MySQL PVC).

**Troubleshooting notes, from this project's own first deployment** (each is a comment in the relevant manifest too, so a future reader hits the explanation in context, not just here):
- **A Service named `kafka` breaks `cp-kafka`.** Kubernetes auto-injects legacy `<SERVICE>_PORT` env vars into every pod for every Service that exists — so a Service literally named `kafka` injects `KAFKA_PORT=tcp://<ip>:9092`, which collides with `cp-kafka`'s own env-to-config mapping and crashes the broker instantly. Fixed with `enableServiceLinks: false` on every pod spec in this project (not just Kafka's) — harmless everywhere, since nothing here relies on that legacy mechanism.
- **Don't use an exec probe that spawns a JVM.** Kafka's readiness probe originally ran `kafka-broker-api-versions`, which starts a second, throwaway JVM admin client on every single probe — stacking real memory on top of the broker's own JVM inside the same container limit, repeatedly triggering `OOMKilled`. A `tcpSocket` probe (confirms the listener is accepting connections, nothing more) fixed it at effectively zero cost.
- **Slow-starting JVMs need a `startupProbe`, not just a lenient `livenessProbe`.** With several Spring Boot services starting concurrently, CPU contention pushed real startup time past what the `livenessProbe`'s `initialDelaySeconds` tolerated — kubelet killed pods that were still legitimately (if slowly) booting, forever restarting them just before they would have finished. A `startupProbe` on every service (generous grace period; `livenessProbe`/`readinessProbe` are suppressed entirely until it succeeds once) fixed it.
- **`spring-boot-starter-mail` auto-configures a health check that can take down the whole app's status.** Spring Boot wires a `MailHealthIndicator` into the aggregate `/actuator/health` response automatically whenever `spring.mail.*` properties are present — so with `SMTP_PASSWORD` unset (the whole point of the `NOTIFICATION_CHANNEL=log` fallback below), `/actuator/health` reported `DOWN`, which every readiness/liveness/startup probe in this project checks. `management.health.mail.enabled=false` (`config-repo/notification-service.properties`) fixed it — the per-notification `SENT`/`FAILED` outcome already recorded by `NotificationSender` (Phase 4) is the correct place for "could we reach Gmail?" to live, not the app's own aggregate health.
- **JVM memory limits need headroom above steady-state usage.** A Spring Boot service's startup phase (class loading, JIT warmup) spikes well above what it settles at afterward — the original `512Mi` limit was fine at rest but caused `OOMKilled` during startup under concurrent load. Every Spring service's limit is `768Mi` (request `384Mi`) for this reason; Kafka's is `1536Mi` with an explicit `KAFKA_HEAP_OPTS=-Xmx512M -Xms512M` so its JVM doesn't sit right at the edge of the container limit.

### Secrets — running this independently of the author

Two secrets exist in this project (`JWT_SECRET`, `SMTP_PASSWORD`), and they are **fundamentally different in kind** — which is why an evaluator cloning this repo doesn't need anything from the author to run it end to end.

**`JWT_SECRET` — self-contained, safe for anyone to generate their own.** Only `auth-service` (issues tokens) and `api-gateway` (verifies them) need to agree on the value; nothing external ever checks it, so any sufficiently random string works.
- Docker Compose: put it in `.env` (git-ignored; `.env.example` documents the variable). If `.env` is missing entirely, `config-repo/application.properties`'s own loud, obviously-insecure placeholder default still lets the stack boot and work.
- Kubernetes: generated inline in the `kubectl create secret` command above (`openssl rand -base64 32`) — nothing is ever written to disk.

**`SMTP_PASSWORD` — a real external credential, and cannot be fabricated.** It's the Gmail App Password for the project's own dedicated sending account (`cakedelight.donotreply@gmail.com`) — see **Real email notifications** above for how the author obtained it. It shouldn't be committed, and it isn't reasonable to hand it to anyone else. The `NotificationSender` seam (Phase 4) exists for exactly this situation:
- **Running your own graded demo?** Put your *own* real `SMTP_PASSWORD` in `.env` (Compose) or your own `kubectl create secret ... --from-literal=SMTP_PASSWORD=...` (Kubernetes), and set `NOTIFICATION_CHANNEL=email`. Real Gmail messages go out, exactly like running the services directly on the host.
- **Evaluating this independently, with no access to that Gmail account?** Leave `SMTP_PASSWORD` unset and `NOTIFICATION_CHANNEL=log` (the default in both `.env.example` and the Kubernetes `cake-delight-env` ConfigMap) — `LoggingNotificationSender` takes over. The full order → notification flow still works and is still fully visible (`GET /api/notifications`, the frontend's notifications page, `channel: "IN_APP"`, `status: "SENT"`) — nothing about FS-7/EV-7 depends on a real email actually arriving.

**Net result: zero secrets are required to run the project end to end.** `SMTP_PASSWORD` is the one optional secret that upgrades an already-working demo to send real email.

## Tech stack

Java 17, Spring Boot 4.1.x, Spring Cloud 2025.1.x, Maven, MySQL, Kafka, Eureka, Spring Cloud Gateway (Server WebMVC variant), JWT (HS256, `jjwt`), BCrypt (`spring-security-crypto`, not the full Spring Security starter), OpenFeign, Next.js. Full rationale and version history in `CLAUDE.md` (local dev notes, not committed).
