# Simplification log

Deliberate record of what was simplified in Cake Delight, and — just as importantly —
what was left complex on purpose. Produced by the `cake-delight-simplify` skill.

## 2026-08-10 — cross-cutting pass across all Spring services (pre-Phase 5)

Files-per-endpoint (`GET /api/catalog/cakes`): **8 → 6**
`CakeController → CakeService → CakeSpecifications → CakeRepository → Cake → CakeResponse`

Java files (main + test, all modules): **152 → 141** | LOC: **5681 → 5548**

| # | Verdict | Change | Effort | Commit |
| --- | --- | --- | --- | --- |
| 1 | COLLAPSE | Merged `CakeServiceImpl` into `CakeService` | MECHANICAL | `cc6227b` |
| 2–6 | COLLAPSE | Dropped the other five single-impl service interfaces (`AuthService`, `RatingService`, `NotificationService`, `BasketService`, `OrderService`) | MECHANICAL | `da7762f` |
| 7 | COLLAPSE | Nested `ValidationFieldError` inside `ErrorResponse` in all five business services | MECHANICAL | `7581fec` |

Verification: every module's unit and slice tests pass (`./mvnw -o test`, excluding the
pre-existing `*ApplicationTests` context failures — see below). Full Phase-4 smoke test
run through the gateway afterwards, including a real order-confirmation email.

### Kept deliberately

| Construct | Why it stays |
| --- | --- |
| 21 custom exception classes | Each carries a distinct error `code` and HTTP status that is part of the published API contract. Error handling is graded directly; the classes are ~8 lines each. |
| One `@RestControllerAdvice` per service | Graded requirement. Never reduced. |
| `OrderCheckedOutEvent` + `OrderCheckoutEventListener` | `@TransactionalEventListener(AFTER_COMMIT)` — stops Kafka announcing an order whose transaction then rolls back. A real durability guarantee, not indirection. |
| `NotificationSender` + `EmailNotificationSender` + `LoggingNotificationSender` | The one interface in the repo with two genuine implementations, selected by `app.notification.channel`. Also the offline-demo fallback when `SMTP_PASSWORD` is unset. |
| The five `*Mapper` classes | Borderline — each is trivial enough to inline. Kept because the project's own `coding-guidelines` convention prescribes a `mapper/` package, and five test classes mock them; the churn was not worth 5 files immediately before Phase 5. Revisit only if the frontend work does not depend on these services. |
| `CakeSpecifications` | Four independently-optional filters on `GET /catalog/cakes`. Derived query methods would need one method per combination. |
| `MutableHttpServletRequest` (gateway) | The servlet-based Spring Cloud Gateway has no built-in header-mutation filter; this is how `X-User-Id` / `X-User-Role` get injected. |
| Duplicated `ErrorResponse` and event DTOs across services | Correct for microservices — CLAUDE.md §10 forbids a shared domain JAR. Duplication here is the design, not a smell. |
| config-server, Eureka, gateway, Kafka, per-service schemas | The graded architecture itself. |

### Blocked

None.

### Pre-existing issue found during the pass (not caused by it)

Every service's `*ApplicationTests.contextLoads` (`@SpringBootTest`) fails without a
running MySQL and config-server, because there is no test profile supplying an
in-memory datasource. `./mvnw test` therefore fails on a clean machine even though all
unit and slice tests pass. Tracked for Phase 5.
