# Database Schema

Per-service schema reference for every service built so far (DL-5). Each service owns one exclusive MySQL schema — no cross-service tables, no foreign keys across service lines (AR-2, AR-5). See `docker/mysql-init/001-init-schemas.sql` for schema creation and each service's `config-repo/<service>.properties` for its datasource.

Tables are created/updated at startup by Hibernate (`spring.jpa.hibernate.ddl-auto=update`, dev only). This document is the source of truth for the intended shape — if it and a running database ever disagree, this file is what should change last, after checking whether the entity class or this doc drifted.

---

## auth-service — `cake_delight_auth_db`

### `users`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `email` | `VARCHAR(150)` | NOT NULL, UNIQUE |
| `password_hash` | `VARCHAR(100)` | NOT NULL — BCrypt hash, never the plaintext password |
| `role` | `VARCHAR(20)` | NOT NULL — enum: `CUSTOMER`, `ADMIN` (stored as string, not ordinal) |
| `created_at` | `TIMESTAMP` | NOT NULL, set once at insert (`@CreationTimestamp`) |

Source: `auth-service/src/main/java/com/cakedelight/authservice/entity/User.java`

Notes:
- Registration always creates `CUSTOMER` — there's no self-serve `ADMIN` signup. Promoting a user requires a manual `UPDATE users SET role = 'ADMIN' WHERE ...` (see README's Phase 3 smoke test for the exact flow).
- `email` uniqueness is enforced at the DB level, not just checked in application code, so a race between two concurrent registrations with the same email can't both succeed.

---

## catalog-service — `cake_delight_catalog_db`

### `cakes`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `name` | `VARCHAR(100)` | NOT NULL |
| `description` | `TEXT` | NOT NULL |
| `category` | `VARCHAR(50)` | NOT NULL |
| `price` | `DECIMAL(10,2)` | NOT NULL — `BigDecimal` in Java, never `double`/`float` |
| `available` | `BOOLEAN` | NOT NULL, default `true` |
| `image_url` | `VARCHAR(255)` | nullable |
| `created_at` | `TIMESTAMP` | NOT NULL, set once at insert (`@CreationTimestamp`) |

Source: `catalog-service/src/main/java/com/cakedelight/catalogservice/entity/Cake.java`

Notes:
- `available` gates `GET /catalog/cakes` (the browse/search endpoint) — unavailable cakes are excluded there but still reachable directly by `GET /catalog/cakes/{id}` and by admin update/delete, since those aren't "browsing".
- Seeded with 14 rows on every startup via `data.sql` (`spring.jpa.defer-datasource-initialization=true` + `spring.sql.init.mode=always`, since MySQL isn't an embedded DB Boot auto-seeds by default).
- No FK from any other service's table into `cakes` — `rating-service` references cakes by plain `cake_id BIGINT`, not a JPA relation.

---

## rating-service — `cake_delight_rating_db`

### `ratings`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `cake_id` | `BIGINT` | NOT NULL — references `catalog-service`'s `cakes.id` by value only, no FK (cross-service DB access is forbidden, CLAUDE.md §10) |
| `user_id` | `BIGINT` | NOT NULL — references `auth-service`'s `users.id` by value only, same reasoning |
| `rating_value` | `INT` | NOT NULL, application-validated `1`–`5` (`@Min`/`@Max` on the request DTO, not a DB `CHECK` constraint) |
| `review_text` | `TEXT` | nullable, application-validated max 2000 characters |
| `created_at` | `TIMESTAMP` | NOT NULL, set once at insert (`@CreationTimestamp`) |

**Unique constraint:** `uk_rating_cake_user` on `(cake_id, user_id)` — one rating per user per cake, enforced at the DB level (not just re-checked in `RatingServiceImpl` before insert, which exists to return a clean `409` instead of a raw constraint-violation `500`).

Source: `rating-service/src/main/java/com/cakedelight/ratingservice/entity/Rating.java`

Notes:
- Purchase verification ("only users who purchased the cake can rate") is deliberately deferred to Phase 4, once `order-service` exists to call — see the dated `TODO` in `RatingServiceImpl.submitRating()`. Right now, any authenticated user can rate any cake once.
- `GET /ratings/cakes/{cakeId}/summary` for a cake with zero ratings returns `{averageRating: 0.0, totalRatings: 0}`, not `NaN`/500 — the average is only queried when `countByCakeId > 0`.

---

## order-service — `cake_delight_order_db`

### `baskets`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `user_id` | `BIGINT` | NOT NULL, UNIQUE — one active basket per user, looked up by `findByUserId` |
| `created_at` | `TIMESTAMP` | NOT NULL, set once at insert (`@CreationTimestamp`) |
| `updated_at` | `TIMESTAMP` | NOT NULL, bumped on every change (`@UpdateTimestamp`) |

### `basket_items`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `basket_id` | `BIGINT` | NOT NULL, FK → `baskets.id` (same-service relation, unlike the cross-service references below) |
| `cake_id` | `BIGINT` | NOT NULL — references `catalog-service`'s `cakes.id` by value only, no FK (CLAUDE.md §10) |
| `cake_name_snapshot` | `VARCHAR(100)` | NOT NULL — captured from catalog-service at add-time |
| `unit_price_snapshot` | `DECIMAL(10,2)` | NOT NULL — captured from catalog-service at add-time; checkout totals off this, not a fresh catalog call (documented simplification, see `OrderServiceImpl.checkout()`) |
| `quantity` | `INT` | NOT NULL, positive |

Source: `order-service/src/main/java/com/cakedelight/orderservice/entity/{Basket,BasketItem}.java`

Notes:
- Adding a cake already in the basket increases its quantity rather than creating a second line (`BasketServiceImpl.addItem()`).
- `catalog-service`'s `available` flag is enforced at add-time via a live Feign call (`404` if the cake doesn't exist, `409` if it exists but isn't available) — see M1 in the Phase 3 audit for why this matters.

### `orders`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `user_id` | `BIGINT` | NOT NULL — references `auth-service`'s `users.id` by value only, no FK |
| `total_amount` | `DECIMAL(10,2)` | NOT NULL — `BigDecimal`, sum of `basket_items.unit_price_snapshot * quantity` at checkout |
| `status` | `VARCHAR(20)` | NOT NULL — enum: `CREATED`, `COMPLETED`. Every order transitions `CREATED`→`COMPLETED` within the checkout transaction; `COMPLETED` is terminal since checkout is a stub (CLAUDE.md §12 — no payment/fulfillment workflow to model further states for) |
| `created_at` | `TIMESTAMP` | NOT NULL, set once at insert |

### `order_items`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `order_id` | `BIGINT` | NOT NULL, FK → `orders.id` |
| `cake_id` | `BIGINT` | NOT NULL — copied from the basket item, not a live reference |
| `cake_name` | `VARCHAR(100)` | NOT NULL |
| `unit_price` | `DECIMAL(10,2)` | NOT NULL |
| `quantity` | `INT` | NOT NULL |

Source: `order-service/src/main/java/com/cakedelight/orderservice/entity/{Order,OrderItem}.java`

Notes:
- On checkout: basket items are copied into a new `Order`/`OrderItem[]`, the basket's items are cleared (the basket row itself is kept, ready to reuse), and an `order.completed` event is published to Kafka — see CLAUDE.md §5.3 and the README's **Eventing (Kafka)** section.
- `order-service` also exposes one internal-only endpoint, `GET /internal/orders/purchases?userId=&cakeId=`, used by `rating-service` to verify a purchase before allowing a rating. Deliberately outside `/orders/**` so the gateway's route for that prefix can't accidentally expose it — see the README's **Internal (service-to-service) endpoints** section.

---

## rating-service — additional note (Phase 4)

`RatingServiceImpl.submitRating()` now calls `order-service`'s internal purchase-check endpoint via Feign before allowing a rating (closing the `TODO(Phase 4)` noted in the Phase 3 audit) — `403 CAKE_NOT_PURCHASED` if the caller never ordered that cake, `503 ORDER_SERVICE_UNAVAILABLE` if order-service can't be reached. No schema change; `ratings` is unchanged from the Phase 3 table above.

---

## notification-service — `cake_delight_notification_db`

### `notifications`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `user_id` | `BIGINT` | NOT NULL — references `auth-service`'s `users.id` by value only, no FK |
| `order_id` | `BIGINT` | NOT NULL — references `order-service`'s `orders.id` by value only, no FK |
| `event_id` | `VARCHAR(36)` | NOT NULL, UNIQUE — the source event's `eventId` (a UUID); not one of CLAUDE.md §5.2's literal columns, but required to satisfy §5.3's idempotency rule ("consumers must handle receiving the same event twice") |
| `channel` | `VARCHAR(20)` | NOT NULL — enum: `EMAIL`, `SMS`, `IN_APP`. Set from `NotificationSender.channel()` (`NotificationServiceImpl`), so it always reflects which sender actually ran — `EMAIL` by default (`EmailNotificationSender`, real Gmail SMTP send), `IN_APP` if `app.notification.channel=log` selects the fallback (`LoggingNotificationSender`) instead |
| `status` | `VARCHAR(20)` | NOT NULL — enum: `SENT`, `FAILED`. Set from whether `NotificationSender.send()` throws (`NotificationServiceImpl`) — a real code path; `EmailNotificationSender` throws on an actual SMTP failure (bad credentials, unreachable host, no `userEmail` on the event), which now makes `FAILED` empirically reachable, not just structurally possible |
| `payload` | `TEXT` | NOT NULL — the full `order.completed` event, serialized as JSON |
| `created_at` | `TIMESTAMP` | NOT NULL, set once at insert |

Source: `notification-service/src/main/java/com/cakedelight/notificationservice/entity/Notification.java`