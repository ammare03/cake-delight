# Cake Delight

A cloud-native cake e-commerce app built as 8 Spring Boot microservices + a Next.js frontend. Capstone project — see `docs/audits/` for progress audits against the requirements.

**Status:** Phase 1 (Infrastructure) — `config-server`, `eureka-server`, and `api-gateway` are up and verified working end-to-end. Business services (`auth-service`, `cake-catalog-service`, `order-service`, `rating-service`, `notification-service`) and the frontend are not started yet.

## Prerequisites

- JDK 17
- Maven 3.9+ (each service also ships its own `mvnw` wrapper)
- Docker Desktop (for MySQL/Kafka, from Phase 4 onward)
- Node.js 20+ (for the frontend, from Phase 5 onward)
- IntelliJ IDEA (or any IDE with Spring Boot support)

## Services and ports

| Service | Port | Purpose |
|---|---|---|
| `config-server` | 8888 | Centralized config, serves everything from `config-repo/` |
| `eureka-server` | 8761 | Service registry — dashboard at `http://localhost:8761` |
| `api-gateway` | **9090** | Single public entry point |

> **Note:** `api-gateway` runs on `9090`, not the more conventional `8080`. This is a local port conflict (XAMPP occupies `8080` on the primary dev machine), not a design decision — if your environment doesn't have that conflict, the port is still `9090` per `config-repo/api-gateway.properties`; change it there if you'd rather use `8080`.

Remaining services aren't built yet; their planned ports are documented in `CLAUDE.md` (not committed — internal dev/agent notes) and will be added here as each one ships.

## Startup order

Services must start in this order — each one depends on the previous being up:

```
1. config-server   (mvn spring-boot:run, or run the main class in your IDE)
2. eureka-server
3. api-gateway
```

Wait for each service to fully start (watch for `Started XxxApplication` in the console) before starting the next. `config-server` and `eureka-server` don't depend on each other, but `api-gateway` needs `config-server` up to fetch its own config (port, routes), and registers itself with `eureka-server` on startup.

Once running:
- Config server: `http://localhost:8888/api-gateway/default` (or any service name) — should return that service's resolved config as JSON.
- Eureka dashboard: `http://localhost:8761` — should show `API-GATEWAY` registered once the gateway is up.
- Gateway placeholder route: `http://localhost:9090/test/actuator/health` — forwards to `eureka-server`'s own health check, proving the gateway actually routes requests. (Temporary — will be replaced by real service routes as each business service ships.)

## Configuration

All runtime config is centralized in `config-repo/`, served by `config-server`. Each service's own `src/main/resources/application.properties` holds only bootstrap essentials (app name, config-server URL) — everything else (ports, datasource, secrets, routes) lives in `config-repo/<service-name>.properties`. `config-server` and `eureka-server` are exceptions: they're foundational infrastructure that starts before anything else could usefully configure them, so their local `application.properties` holds their real settings directly (this is deliberate, not an oversight).

Config format is `.properties`, not YAML, project-wide.

**Secrets:** for local development, plaintext values (DB passwords, JWT secret) will live directly in `config-repo/*.properties` once those services exist — acceptable at this scope but never committed for anything beyond local dev. From Phase 6 onward, these move to environment variables locally and Kubernetes `Secret` resources in-cluster.

## Tech stack

Java 17, Spring Boot 4.1.x, Spring Cloud 2025.1.x, Maven, MySQL, Kafka, Eureka, Spring Cloud Gateway (Server WebMVC variant), JWT (HS256), OpenFeign, Next.js. Full rationale and version history in `CLAUDE.md` (local dev notes, not committed).
