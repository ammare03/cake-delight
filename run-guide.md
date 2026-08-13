# Run Guide

Operational companion to **[README.md](README.md)** (architecture, API reference), **[db-schema.md](db-schema.md)** (table-level schema), and **[event-contract.md](event-contract.md)** (the `order.completed` payload) — none of that is repeated here. This file only covers *how to run the stack*, in three ways: host/IDE, Docker Compose, Kubernetes.


## Contents

- [1. Local (host / IDE)](#1-local-host--ide)
- [2. Docker Compose](#2-docker-compose)
- [3. Kubernetes (Docker Desktop)](#3-kubernetes-docker-desktop)
- [Environment variables](#environment-variables)
- [Troubleshooting](#troubleshooting)

---

## 1. Local (host / IDE)

Every Spring service and the Next.js app run as normal processes (`mvnw spring-boot:run` / `npm run dev`); only MySQL (+ Kafka/Zookeeper) run in Docker.

**Prerequisites:** JDK 17, Docker Desktop, Node.js 20+. Maven isn't needed separately — each service ships its own `mvnw`/`mvnw.cmd`.

**Start order matters:** every service fetches its config from `config-server` and registers with `eureka-server` at startup, and `api-gateway` resolves routes by looking services up in Eureka — so start `config-server` → `eureka-server` → the five business services (any order) → `api-gateway` last.

```powershell
# Infrastructure only — naming these three explicitly avoids also starting
# the containerized Spring services, which would fight the host-run ones for ports
docker compose up -d mysql zookeeper kafka
docker compose ps   # wait for mysql "healthy"

# Compose publishes MySQL on 3307, but each service's own default falls back
# to 3309 — set this in every terminal window before starting a Spring service
$env:DB_PORT = "3307"

# Only the notification-service terminal needs this — leave unset to fall
# back to recording notifications instead of emailing them
$env:SMTP_PASSWORD = "your16charapppassword"
```

One terminal per service, in order:

```powershell
cd config-server; .\mvnw.cmd spring-boot:run
# wait for "Started EurekaServerApplication", then:
cd eureka-server; .\mvnw.cmd spring-boot:run

# any order among these five — remember $env:DB_PORT (and $env:SMTP_PASSWORD
# for notification-service) in each new terminal
cd auth-service; .\mvnw.cmd spring-boot:run
cd catalog-service; .\mvnw.cmd spring-boot:run
cd order-service; .\mvnw.cmd spring-boot:run
cd rating-service; .\mvnw.cmd spring-boot:run
cd notification-service; .\mvnw.cmd spring-boot:run

cd api-gateway; .\mvnw.cmd spring-boot:run   # last — resolves lb:// routes to already-registered services

cd frontend-service; copy .env.local.example .env.local; npm install; npm run dev   # optional, UI only
```

**Verify:**
- Eureka dashboard `http://localhost:8761` — shows `AUTH-SERVICE`, `CATALOG-SERVICE`, `ORDER-SERVICE`, `RATING-SERVICE`, `NOTIFICATION-SERVICE`, `API-GATEWAY`.
- `curl http://localhost:9090/api/catalog/cakes` — returns the seeded cakes.
- Frontend at `http://localhost:3000`.

---

## 2. Docker Compose

Builds and runs the whole stack (MySQL, Zookeeper, Kafka, all 8 Spring services, the frontend) as containers on one Docker network.

**Prerequisites:** Docker Desktop.

```powershell
# If this repo lives under a cloud-synced folder (OneDrive, Dropbox, Google
# Drive), BuildKit's file-transfer protocol fails against it with
# "invalid file request <path>" — set these first. Harmless no-ops otherwise.
$env:DOCKER_BUILDKIT = "0"
$env:COMPOSE_DOCKER_CLI_BUILD = "0"

# Optional — only if you want real Gmail delivery instead of the "log" fallback
$env:SMTP_PASSWORD = "your16charapppassword"
$env:NOTIFICATION_CHANNEL = "email"

docker compose build
docker compose up -d
docker compose ps      # wait for everything "Up (healthy)"
```

Copying `.env.example` to `.env` works the same way as the `$env:SMTP_*`/`$env:NOTIFICATION_CHANNEL` lines above, if you'd rather not re-set them per terminal window.

**Verify:**
- `curl http://localhost:9090/api/catalog/cakes` (gateway) and `http://localhost:3000` (frontend) — same URLs as host dev.
- `docker compose logs -f notification-service` — confirms sends (or `log`-channel fallback records).

Stop with `docker compose down` (add `-v` to also drop the MySQL volume).

---

## 3. Kubernetes (Docker Desktop)

Tested against Docker Desktop's built-in Kubernetes. `k8s/` manifests: `namespace.yaml`, `config/`, `secrets/`, `mysql/`, `kafka/`, `services/*.yaml` (one Deployment + Service per component).

**Prerequisites:** Docker Desktop with Kubernetes enabled; `kubectl config current-context` reports `docker-desktop`. If Compose is running, stop it first (`docker compose stop`) — both share the same Docker Desktop VM.

```powershell
# See the cloud-synced-folder note in the Docker Compose section above —
# applies here too, since these are the same `docker build` calls.
$env:DOCKER_BUILDKIT = "0"
$env:COMPOSE_DOCKER_CLI_BUILD = "0"

# Build the 8 Spring images (same images/tags Compose uses)
docker compose build

# Frontend needs its own build — NEXT_PUBLIC_API_BASE_URL is baked in at build
# time, and in-cluster the gateway is reachable at its NodePort, not :9090
docker build `
  --build-arg NEXT_PUBLIC_API_BASE_URL=http://localhost:30090/api `
  -t cake-delight/frontend-service:latest ./frontend-service

# Namespace + non-secret config
kubectl apply -f k8s/namespace.yaml
kubectl create configmap cake-delight-config-repo --from-file=config-repo/ -n cake-delight
kubectl apply -f k8s/config/env-configmap.yaml -n cake-delight
```

**Secret** — type this directly into your own terminal. `cake-delight-env` (applied above) sets `NOTIFICATION_CHANNEL=email` by default for Kubernetes, so `SMTP_PASSWORD` here is a real Gmail App Password, not a placeholder — see **Real email notifications** in README.md for how to get one:

```powershell
kubectl create secret generic cake-delight-secrets `
  --from-literal=JWT_SECRET="REPLACE_WITH_openssl_rand_-base64_32_OUTPUT" `
  --from-literal=SMTP_PASSWORD="REPLACE_WITH_YOUR_16_CHAR_APP_PASSWORD" `
  -n cake-delight
```

Use '[Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Minimum 0 -Maximum 256) }))' instead of 'openssl rand -base64 32' if you do not have openssl installed locally.

Because the value is baked into the Secret before any pod is created, `notification-service` picks it up on its very first boot — no ConfigMap patch or rollout restart needed. If you'd rather not configure real email for a given run, edit `NOTIFICATION_CHANNEL` to `log` in `k8s/config/env-configmap.yaml` before applying it, and `SMTP_PASSWORD` above can be left as an empty string.

**Deploy, in order** — MySQL/Kafka, then `config-server`/`eureka-server`, then business services, then gateway/frontend:

```powershell
kubectl apply -f k8s/mysql/mysql.yaml -n cake-delight
kubectl apply -f k8s/kafka/kafka.yaml -n cake-delight

kubectl apply -f k8s/services/config-server.yaml -n cake-delight
kubectl apply -f k8s/services/eureka-server.yaml -n cake-delight
kubectl rollout status deployment/config-server -n cake-delight
kubectl rollout status deployment/eureka-server -n cake-delight

kubectl apply -f k8s/services/auth-service.yaml -n cake-delight
kubectl apply -f k8s/services/catalog-service.yaml -n cake-delight
kubectl apply -f k8s/services/order-service.yaml -n cake-delight
kubectl apply -f k8s/services/rating-service.yaml -n cake-delight
kubectl apply -f k8s/services/notification-service.yaml -n cake-delight

kubectl apply -f k8s/services/api-gateway.yaml -n cake-delight
kubectl apply -f k8s/services/frontend-service.yaml -n cake-delight
```

On a machine with 4 CPU cores or fewer allocated to Docker Desktop, expect the five business-service pods to take several minutes each to pass their startup probe the first time — they're all booting concurrently and genuinely competing for CPU, not stuck. `kubectl get pods -n cake-delight -w` to watch them come up rather than assuming a restart means something is broken.

**Verify:**
- `kubectl get pods -n cake-delight` — everything `1/1 Running`.
- `curl http://localhost:30090/api/catalog/cakes` (gateway NodePort) and `http://localhost:30300` (frontend NodePort).
- `kubectl logs -f deployment/notification-service -n cake-delight`.

Teardown: `kubectl delete namespace cake-delight`.

---

## Environment variables

Every service reads config via Spring's `${VAR:default}` placeholder syntax — absent means "use the default", present-but-empty does **not** fall back (relevant for `JWT_SECRET` below).

| Variable | Default (unset) | Compose value | Kubernetes value | Used by |
|---|---|---|---|---|
| `DB_HOST` | `localhost` | `mysql` | `mysql` | every business service |
| `DB_PORT` | `3309` | `3306` (in-network) | `3306` | every business service — **host mode must set `3307`** to match Compose's published MySQL port |
| `DB_PASSWORD` | *(empty)* | *(empty)* | *(empty)* | every business service — MySQL runs with an empty root password everywhere |
| `CONFIG_SERVER_HOST` | `localhost` | `config-server` | `config-server` | every service except `config-server`/`eureka-server` |
| `CONFIG_SERVER_PORT` | `8888` | `8888` | `8888` | same |
| `CONFIG_REPO_PATH` | `<repo>/config-repo` | `/config-repo` (bind mount) | `/config-repo` (ConfigMap volume) | `config-server` only |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | `http://eureka-server:8761/eureka/` | `http://eureka-server:8761/eureka/` | every service |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | `kafka:29092` | `kafka:9092` | `order-service`, `notification-service` |
| `JWT_SECRET` | insecure loud placeholder | same insecure placeholder unless set | must come from the `cake-delight-secrets` Secret — pods won't start without it | `auth-service` (issues), `api-gateway` (validates) — **must be identical on both** |
| `SMTP_USERNAME` | `cakedelight.donotreply@gmail.com` | inherited | inherited | `notification-service` |
| `SMTP_PASSWORD` | *(empty)* | *(empty)* | **required** — a real Gmail App Password | `notification-service` — leave unset locally/Compose to run with the log fallback; Kubernetes defaults to `email` channel, so this must be a real value there (or set `NOTIFICATION_CHANNEL=log` in `k8s/config/env-configmap.yaml` instead) |
| `NOTIFICATION_CHANNEL` | `email` | `log` | `email` | `notification-service` — `email` (real SMTP send) or `log` (console/DB only, no credentials needed) |
| `FRONTEND_ORIGIN` | `http://localhost:3000` | `http://localhost:3000` | `http://localhost:30300` | `api-gateway` (CORS) |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:9090/api` | `http://localhost:9090/api` | `http://localhost:30090/api` | `frontend-service` — **build-time only**, baked into the image |

Locally, copy `.env.example` → `.env` (root) and `frontend-service/.env.local.example` → `frontend-service/.env.local` to set these without exporting them per terminal.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| A host-run service fails to connect to MySQL | `DB_PORT` still defaults to `3309`, Compose publishes MySQL on `3307` | `$env:DB_PORT = "3307"` in that terminal before starting the service |
| `api-gateway` returns `503`/routes fail right after startup | It started before the business services registered with Eureka | Wait ~30s for re-registration, or just start the gateway last (see start order) |
| Login succeeds but every subsequent request is `401` | `JWT_SECRET` differs between `auth-service` and `api-gateway` (e.g. set in only one terminal/Secret) | Use the same value for both — in Kubernetes both read it from the one `cake-delight-secrets` Secret already |
| Notifications recorded as `FAILED` (Kubernetes) | `SMTP_PASSWORD` in `cake-delight-secrets` is empty, wrong, or the account isn't set up for App Passwords (see README's **Real email notifications**) — `NOTIFICATION_CHANNEL` is already `email` by default here | Recreate the Secret with a real App Password: `kubectl delete secret cake-delight-secrets -n cake-delight` then re-run the `kubectl create secret` command with the correct value, then `kubectl rollout restart deployment/notification-service -n cake-delight` |
| Notifications recorded as `FAILED`, or `channel` is `IN_APP` when you expected `EMAIL` (local/Compose) | No `SMTP_PASSWORD` set, or `NOTIFICATION_CHANNEL` still `log` | Set `SMTP_PASSWORD` and `NOTIFICATION_CHANNEL=email` in that terminal/`.env` before starting `notification-service` |
| Business-service pods stuck restarting (`0/1 Running`, restart count climbing), possibly with two pods for one Deployment | Docker Desktop has 4 CPU cores or fewer — 5+ Spring Boot JVMs booting at once genuinely can't get enough CPU each, so they miss the startup probe's deadline and get killed mid-boot, right before they'd have become ready. Two pods for one Deployment means a rollout got triggered (e.g. a manual `kubectl rollout restart`) while the first pod was still fighting for CPU | Just wait — `kubectl get pods -n cake-delight -w`, give it 5–10+ minutes. If it's still failing after that, `kubectl describe pod <name> -n cake-delight` and check the `Events` section for `failed startup probe`; the manifests already give each service ~15 minutes (`startupProbe.failureThreshold: 180`) before giving up, which should be enough even under heavy contention |
| Kubernetes pods `Pending`/`CrashLoopBackOff` on `config-server` | `cake-delight-config-repo` ConfigMap wasn't created (or is stale after editing `config-repo/`) | `kubectl create configmap cake-delight-config-repo --from-file=config-repo/ -n cake-delight` (delete the old one first if it already exists) |
| Frontend calls the wrong gateway URL / CORS errors | `NEXT_PUBLIC_API_BASE_URL` is baked in at image build time, not runtime | Rebuild the frontend image with the correct `--build-arg` for that environment — can't be fixed by changing a running container's env |
| `docker compose build`/`docker build` fails with `invalid file request <path>` | BuildKit's file-transfer protocol doesn't like OneDrive/Dropbox-synced paths — the repo is inside one | `$env:DOCKER_BUILDKIT = "0"; $env:COMPOSE_DOCKER_CLI_BUILD = "0"` before building (already in the command blocks above) |
