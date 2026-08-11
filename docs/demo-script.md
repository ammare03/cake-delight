# End-to-End Demo Script (DL-8)

A rehearsed walkthrough of the full customer journey — FS-1 through FS-7 as one continuous path (EV-7) — runnable against any of the three environments this project supports: services on the host, Docker Compose, or Kubernetes. Takes under two minutes once the stack is up.

**Never hardcode a cake id.** `catalog-service`'s `data.sql` reseeds on every restart (`DELETE FROM cakes` + re-insert), so ids shift across restarts — this script resolves one from a live `GET` response at step 3, same as `frontend-service` already does everywhere (see the Phase 5 audit's N6 finding, `docs/audits/audit-2026-08-11-phase5.md`).

## Before you start

Pick one base URL and use it for every step below:

| Environment | Base URL |
|---|---|
| Host (IDE-run services) | `http://localhost:9090/api` |
| Docker Compose | `http://localhost:9090/api` |
| Kubernetes (Docker Desktop) | `http://localhost:30090/api` |

The frontend, if you want the UI version of this same script instead of curl: `http://localhost:3000` (host/Compose) or `http://localhost:30300` (Kubernetes).

```sh
export BASE=http://localhost:9090/api   # or :30090 for Kubernetes
```

## Script

**1. Register.**
```sh
curl -s -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"password123"}'
```
Expect `201` and a JWT in the response. Save it:
```sh
export TOKEN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
```

**2. Browse the catalog (no token).**
```sh
curl -s $BASE/catalog/cakes
```
Expect `200` and the ~14 seeded cakes.

**3. Filter, and resolve a real cake id.**
```sh
export CAKE_ID=$(curl -s "$BASE/catalog/cakes?category=chocolate" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo "Using cakeId=$CAKE_ID"
```
Expect a filtered subset (only `category=chocolate` cakes) and a real numeric id.

**4. Add to basket.**
```sh
curl -s -X POST $BASE/orders/basket/items -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d "{\"cakeId\":$CAKE_ID,\"quantity\":2}"
```
Expect `201` and a basket with one line, `totalAmount` = unit price × 2.

**5. Checkout.**
```sh
curl -s -X POST $BASE/orders/checkout -H "Authorization: Bearer $TOKEN"
```
Expect `201`, `status: "COMPLETED"`, and the same total as step 4. Note the returned `id` — this is `$ORDER_ID` for reference, not needed for the next steps.

**6. Confirm the notification (Kafka → notification-service).**
```sh
curl -s $BASE/notifications -H "Authorization: Bearer $TOKEN"
```
Expect `200` and one row, `orderId` matching step 5, `status: "SENT"`. `channel` is `"EMAIL"` if the deployment has a real `SMTP_PASSWORD` configured, `"IN_APP"` otherwise (see the root README's **Secrets** section) — both are a pass; the point being demonstrated is that the event was consumed and recorded, not which channel handled it.

**7. Rate the purchased cake.**
```sh
curl -s -X POST $BASE/ratings -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"cakeId\":$CAKE_ID,\"ratingValue\":5,\"reviewText\":\"Great cake!\"}"
```
Expect `201` — this only succeeds because `$CAKE_ID` was actually purchased in step 4/5 (`rating-service` verifies via a Feign call to `order-service`'s `/internal/orders/purchases` endpoint).

**8. Confirm the average updates.**
```sh
curl -s $BASE/ratings/cakes/$CAKE_ID/summary -H "Authorization: Bearer $TOKEN"
```
Expect `{"averageRating":5.0,"totalRatings":1}`.

## What this proves

| Step | Requirement |
|---|---|
| 1 | FS-1 (registration), AR-1 (gateway is the only entry point) |
| 2, 3 | FS-1, FS-2 (browse, filter) |
| 4 | FS-3, SD-O1/O2 (basket, server-resolved price via Feign to `catalog-service`) |
| 5 | FS-5, SD-O3/O4 (checkout, status transition, `order.completed` published after commit) |
| 6 | FS-7, SD-N1–N3, AR-3 (event consumed asynchronously, recorded with a real status) |
| 7 | FS-6, SD-R1 (rating, purchase-verified via Feign to `order-service`) |
| 8 | SD-R3 (average + count, correct on a freshly-rated cake) |

## In-cluster specifics (Kubernetes)

Running this against `:30090` additionally demonstrates AR-10/EV-5: the request crosses the gateway's `NodePort`, into a `ClusterIP`-only business service, resolved via Kubernetes Service DNS (not `localhost`) end to end — `kubectl get pods -n cake-delight` alongside the script shows `catalog-service` serving from either of its 2 replicas, proving no basket/session state is held in a pod's memory (AR-7).
