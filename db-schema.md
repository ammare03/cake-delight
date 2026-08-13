# Database Schema

Part of the Cake Delight docs — see [README.md](README.md) for architecture and API, [run-guide.md](run-guide.md) for setup/deployment, [event-contract.md](event-contract.md) for the `order.completed` payload.

Five MySQL schemas, one per business service. Each is owned exclusively by its service — no service ever queries another's tables, and there are **no foreign keys across schemas** (see README's [Data ownership](README.md#data-ownership)). Cross-service references (e.g. a `cake_id` or `user_id` from another service) are stored as plain, unconstrained columns.

Tables are generated from the JPA `@Entity` classes by `spring.jpa.hibernate.ddl-auto=update` (dev only). The schemas themselves — the empty databases — are created by [`docker/mysql-init/001-init-schemas.sql`](docker/mysql-init/001-init-schemas.sql) when running via Docker Compose, or manually otherwise.

| Schema | Owning service | Real DB name |
|---|---|---|
| auth | `auth-service` | `cake_delight_auth_db` |
| catalog | `catalog-service` | `cake_delight_catalog_db` |
| order | `order-service` | `cake_delight_order_db` |
| rating | `rating-service` | `cake_delight_rating_db` |
| notification | `notification-service` | `cake_delight_notification_db` |

---

## auth-service — `cake_delight_auth_db`

Stores user identity and credentials: who can log in, their password hash, and their role. No other service can see a password hash.

| Table | Purpose |
|---|---|
| `users` | One row per registered user. |

### `users`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `email` | `VARCHAR(150)` | NOT NULL, UNIQUE |
| `password_hash` | `VARCHAR(100)` | NOT NULL — BCrypt hash |
| `role` | `VARCHAR(20)` | NOT NULL — `CUSTOMER` or `ADMIN` |
| `created_at` | `TIMESTAMP` | NOT NULL, set on insert |

**Constraints:** `email` is unique at the DB level (not just app-checked). No cross-schema foreign keys.

---

## catalog-service — `cake_delight_catalog_db`

Stores the cake product catalog — the read-heavy data users browse and filter.

| Table | Purpose |
|---|---|
| `cakes` | One row per cake product. |

### `cakes`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `name` | `VARCHAR(100)` | NOT NULL |
| `description` | `TEXT` | NOT NULL |
| `category` | `VARCHAR(50)` | NOT NULL |
| `price` | `DECIMAL(10,2)` | NOT NULL |
| `available` | `BOOLEAN` | NOT NULL, default `true` |
| `image_url` | `VARCHAR(255)` | nullable |
| `created_at` | `TIMESTAMP` | NOT NULL, set on insert |

**Constraints:** none beyond the column-level ones above. No cross-schema foreign keys.

**Seed data:** `catalog-service/src/main/resources/data.sql` reseeds ~14 sample cakes on every startup (dev only).

---

## order-service — `cake_delight_order_db`

Stores baskets and orders — the transactional core of checkout. Baskets and orders live in the same schema because checkout reads a basket and writes an order inside a single local transaction.

| Table | Purpose |
|---|---|
| `baskets` | One active basket per user. |
| `basket_items` | Line items in a basket. |
| `orders` | A completed checkout. |
| `order_items` | Line items in an order (snapshot, not linked to `basket_items`). |

### `baskets`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `user_id` | `BIGINT` | NOT NULL, UNIQUE — one basket per user |
| `created_at` | `TIMESTAMP` | NOT NULL, set on insert |
| `updated_at` | `TIMESTAMP` | NOT NULL, bumped on every change |

### `basket_items`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `basket_id` | `BIGINT` | NOT NULL, FK → `baskets.id` |
| `cake_id` | `BIGINT` | NOT NULL — references catalog-service's `cakes.id` by value only, no FK |
| `cake_name_snapshot` | `VARCHAR(100)` | NOT NULL — captured from catalog-service at add-time |
| `unit_price_snapshot` | `DECIMAL(10,2)` | NOT NULL — captured from catalog-service at add-time |
| `quantity` | `INT` | NOT NULL |

### `orders`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `user_id` | `BIGINT` | NOT NULL — references auth-service's `users.id` by value only, no FK |
| `total_amount` | `DECIMAL(10,2)` | NOT NULL |
| `status` | `VARCHAR(20)` | NOT NULL — `CREATED` or `COMPLETED` |
| `created_at` | `TIMESTAMP` | NOT NULL, set on insert |

### `order_items`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `order_id` | `BIGINT` | NOT NULL, FK → `orders.id` |
| `cake_id` | `BIGINT` | NOT NULL — copied from the basket item, not a live reference |
| `cake_name` | `VARCHAR(100)` | NOT NULL |
| `unit_price` | `DECIMAL(10,2)` | NOT NULL |
| `quantity` | `INT` | NOT NULL |

**Constraints:** `basket_items.basket_id` → `baskets.id` and `order_items.order_id` → `orders.id` are real, in-schema foreign keys. `cake_id` (both tables) and `orders.user_id` reference other services' rows by value only — no cross-schema foreign keys.

---

## rating-service — `cake_delight_rating_db`

Stores user ratings and reviews for cakes.

| Table | Purpose |
|---|---|
| `ratings` | One row per user's rating of one cake. |

### `ratings`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `cake_id` | `BIGINT` | NOT NULL — references catalog-service's `cakes.id` by value only, no FK |
| `user_id` | `BIGINT` | NOT NULL — references auth-service's `users.id` by value only, no FK |
| `rating_value` | `INT` | NOT NULL, app-validated 1–5 |
| `review_text` | `TEXT` | nullable |
| `created_at` | `TIMESTAMP` | NOT NULL, set on insert |

**Constraints:** unique constraint `uk_rating_cake_user` on `(cake_id, user_id)` — one rating per user per cake, enforced at the DB level. No cross-schema foreign keys.

---

## notification-service — `cake_delight_notification_db`

Stores a record of every notification sent (or attempted) for a completed order, built from the `order.completed` Kafka event.

| Table | Purpose |
|---|---|
| `notifications` | One row per processed `order.completed` event. |

### `notifications`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `user_id` | `BIGINT` | NOT NULL — references auth-service's `users.id` by value only, no FK |
| `order_id` | `BIGINT` | NOT NULL — references order-service's `orders.id` by value only, no FK |
| `event_id` | `VARCHAR(36)` | NOT NULL, UNIQUE — the source event's `eventId` (UUID) |
| `channel` | `VARCHAR(20)` | NOT NULL — `EMAIL`, `SMS`, or `IN_APP` |
| `status` | `VARCHAR(20)` | NOT NULL — `SENT` or `FAILED` |
| `payload` | `TEXT` | NOT NULL — the full event, serialized as JSON |
| `created_at` | `TIMESTAMP` | NOT NULL, set on insert |

**Constraints:** `event_id` is unique at the DB level — this is the idempotency check that lets the Kafka consumer safely receive the same event twice (see README's [Event contract](README.md#event-contract)). No cross-schema foreign keys.
