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

## Not yet built

`order-service` (`order_db`) and `notification-service` (`notification_db`) don't exist yet — their schemas will be documented here as part of Phase 4, per CLAUDE.md §5.2's table definitions (`baskets`, `basket_items`, `orders`, `order_items`, `notifications`).
