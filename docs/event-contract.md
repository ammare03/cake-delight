# Event Contract

Per-topic reference for every Kafka event in Cake Delight (DL-6). One topic exists so far — see CLAUDE.md §5.3 for the original spec this was built from. Mirrors `docs/db-schema.md`'s structure: this file is the single canonical place the contract lives; `CLAUDE.md` and the README's Eventing section describe the same thing in prose and should stay consistent with this, not duplicate it in detail.

---

## `order.completed`

**Producer:** `order-service`, from `OrderServiceImpl.checkout()` — published via an internal `OrderCheckedOutEvent` Spring application event, sent to Kafka only after the checkout transaction commits (`OrderCheckoutEventListener`, `@TransactionalEventListener(phase = AFTER_COMMIT)`). Never published for a checkout that rolls back.

**Consumer:** `notification-service`, `OrderCompletedListener` (`@KafkaListener`), consumer group `notification-service`.

**Key:** `orderId` (as a `String`) — keeps every message for one order on the same partition. Not load-bearing with the single partition this topic currently has (see `order-service`'s `KafkaTopicConfig`), but correct if the topic is ever repartitioned.

**Delivery semantics:** Kafka's default at-least-once. The consumer is idempotent, keyed on `eventId` (`NotificationRepository.existsByEventId`, backed by a DB-unique `event_id` column) — a redelivery is detected and skipped, never double-recorded.

### Payload

```json
{
  "eventId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "eventType": "ORDER_COMPLETED",
  "occurredAt": "2026-08-10T10:30:00Z",
  "orderId": 123,
  "userId": 45,
  "userEmail": "user@example.com",
  "totalAmount": 1250.00,
  "items": [
    { "cakeId": 5, "cakeName": "Chocolate Truffle", "quantity": 2, "unitPrice": 500.00 }
  ]
}
```

| Field | Type | Notes |
|---|---|---|
| `eventId` | `String` (UUID) | Generated fresh per publish (`UUID.randomUUID()`); the idempotency key |
| `eventType` | `String` | Always `"ORDER_COMPLETED"` — reserved for if a second event type is ever added to this topic |
| `occurredAt` | `Instant` (ISO-8601) | When `order-service` built the event, not when the order was created |
| `orderId` | `Long` | `order-service`'s `orders.id` |
| `userId` | `Long` | `auth-service`'s `users.id`, by value only — no callback needed to resolve it |
| `userEmail` | `String`, nullable | From the gateway-injected `X-User-Email` header at checkout time; null if the caller's JWT had no `email` claim |
| `totalAmount` | `BigDecimal` | Serializes as a JSON number; consumer must deserialize as `BigDecimal`, never `double`/`float` |
| `items` | `Item[]` | See below |

`Item`:

| Field | Type | Notes |
|---|---|---|
| `cakeId` | `Long` | `catalog-service`'s `cakes.id`, by value only |
| `cakeName` | `String` | Snapshotted at add-to-basket time, not a live catalog lookup |
| `quantity` | `Integer` | |
| `unitPrice` | `BigDecimal` | Snapshotted at add-to-basket time |

**Self-contained by design:** every field a consumer could plausibly need is in the payload. `notification-service` never calls back to `order-service`, `catalog-service`, or `auth-service` to enrich this event — that's deliberate, not an oversight (CLAUDE.md §5.3).

**Serialization:** JSON via `spring-kafka`'s `JsonSerializer`/`JsonDeserializer`. The producer disables type headers (`spring.json.add.type.headers=false`) since the producer's and consumer's copies of `OrderCompletedEvent` are two independent classes in two independent packages (CLAUDE.md §10 — no shared domain-model JAR), so a Java type header naming the producer's class would mean nothing on the consumer side. The consumer is told what to deserialize into via `spring.json.value.default.type` instead. Both sides additionally wrap their (de)serializers in Spring Kafka's `ErrorHandlingDeserializer` (consumer side) so a malformed message is logged and skipped by the container's error handler rather than deserializing outside the listener and looping the consumer on a poison message.

### Versioning note

No versioning scheme exists yet — there's only ever been one shape for this payload. If a breaking change is needed later (a field removed or its type changed, not just a new optional field added), the cheapest option at this project's scale is a new field, e.g. `schemaVersion`, defaulted to `1` and checked by the consumer; a new topic (`order.completed.v2`) is the heavier option, only worth it for a genuinely incompatible change. Prefer additive, optional fields over either when possible — the consumer already tolerates unknown fields by virtue of using Jackson's default (non-failing) deserialization behavior.

### Config

`spring.kafka.bootstrap-servers` and `app.kafka.topics.order-completed` (the topic name) live in the **shared** `config-repo/application.properties` — both `order-service` and `notification-service` need the identical values. Producer-specific settings (serializers) live in `config-repo/order-service.properties`; consumer-specific settings (deserializers, consumer group id, offset reset policy) live in `config-repo/notification-service.properties`.
