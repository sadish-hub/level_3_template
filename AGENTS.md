# AGENTS.md — Level 3 Restaurant Order Lab

Working agreements for agents implementing or changing this project. Product requirements live in [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md); track progress in [`docs/REQUIREMENTS_CHECKLIST.md`](docs/REQUIREMENTS_CHECKLIST.md). Multi-phase roadmap: [`docs/plans/master_plan.md`](docs/plans/master_plan.md) (write detailed `docs/plans/phase-N-….md` plans one phase at a time; do not skip TDD).

---

## Project purpose & stack

Event-sourced restaurant order flow (CQRS) built with **Axon Framework** and **Spring Boot**.

| Concern | Detail |
|--------|--------|
| Package root | `com.nexusgroup.restaurant` |
| Java / Axon / Boot | Java 21, Axon 4.12, Spring Boot 3.5 |
| Build | Maven (`./mvnw` or `mvn`) |
| Messaging / ES store | Axon Server (Docker Compose) |
| Schedules / deadlines support | JobRunr + Axon `DeadlineManager` |
| Runtime loop | Every 15s, `FoodOrderScheduler` sends a random `CreateFoodOrderCommand` |

**Implement** the Level 3 flow end-to-end using the provided commands and events:

1. Create order → `FoodOrderCreatedEvent`
2. Saga starts cooking → `CookDishCommand` → kitchen success/failure events
3. Complete or cancel the order (including deadline/timeout paths)

**Operators** inspect commands/events via Axon Server UI: [http://localhost:8024](http://localhost:8024) (start standalone node after `docker-compose up -d`).

---

## What you may vs must not modify

### Must not modify (locked — marked `DO NOT EDIT`)

| Area | Path / type |
|------|-------------|
| Config | `config/` (e.g. `StorageProviderConfiguration`) |
| Scheduler | `scheduler/FoodOrderScheduler` |
| Commands | `command/*` — Create / Cook / Complete / Cancel |
| Events | `event/*` — Created / DishCooked / CookingFailed / Completed / Cancelled |
| App entry | `RestaurantApplication` |
| Kitchen client API | `service/KitchenClient`, `service/KitchenException` |
| Value types | `aggregate/value/Dish`, `aggregate/value/OrderState` (`CREATED` \| `COMPLETED` \| `CANCELLED`) |

Do not rename fields, change record shapes, or invent parallel command/event types. Honor `@TargetAggregateIdentifier` and existing contracts.

### May / should modify

| Area | Path |
|------|------|
| Aggregate | `aggregate/FoodOrderAggregate` |
| Saga / process manager | `saga/FoodOrderSaga` |
| Kitchen command handler | `service/KitchenService` |
| Unit tests | `src/test/.../aggregate/FoodOrderAggregateTest`, `.../saga/FoodOrderSagaTest`, kitchen service tests |
| E2E / integration tests | `src/test/.../e2e/` (`FoodOrderFlowE2ETest`, in-memory Axon test config) |
| Docs | `docs/*`, this `AGENTS.md`, and root `README` if present |

Keep `pom.xml`, Docker Compose, and Axon wiring changes minimal and justified; prefer implementing against the existing stack.

---

## Coding standards

- Prefer clarity and small, reviewable diffs over clever abstractions.
- Match existing style: Lombok where already used, package layout, Axon annotations, logging patterns.
- Minimal scope: implement only what the current requirement needs. No drive-by refactors, renames, or dependency churn.
- Event-sourced aggregate: command handlers validate and apply events; state is rebuilt via `@EventSourcingHandler` — do not persist aggregate state outside the event stream.
- Saga: associate on order id, dispatch commands via `CommandGateway`, use `DeadlineManager` for cooking timeouts / cleanup; keep gateway/deadline fields `transient` as scaffolded.
- `KitchenService`: handle `CookDishCommand`, call `KitchenClient.cookDish`, publish `DishCookedEvent` or `CookingDishFailedEvent` via `EventGateway` (catch `KitchenException`). Failure reason = `KitchenException.getMessage()`.
- Enforce valid transitions (`OrderState`); reject invalid commands rather than silently no-oping when Axon state-transition guidance applies.

---

## TDD workflow

**No production behavior without a failing test first.**

1. **Red** — Write one focused failing test for the next behavior (aggregate fixture, saga fixture, kitchen unit test, or e2e scenario).
2. **Verify red** — Run it; confirm it fails because the feature is missing (not because of a typo/setup bug).
3. **Green** — Write the smallest implementation that passes.
4. **Refactor** — Clean up only while staying green; do not add untested behavior.
5. Repeat for the next behavior (happy path, then edge cases).

If you wrote implementation first, delete it and start from a failing test. Prefer Axon `AggregateTestFixture` / `SagaTestFixture` for domain behavior; mock only when unavoidable (e.g. isolating kitchen failure).

---

## Testing expectations

### Unit tests (required)

Use the existing scaffolds and expand them:

- **`FoodOrderAggregateTest`** — create → created; complete / cancel from valid states; **invalid transitions** (e.g. complete/cancel when not allowed); cancel **reason** propagated on `FoodOrderCancelledEvent`.
- **`FoodOrderSagaTest`** — on created → `CookDishCommand`; on cooked → `CompleteFoodOrderCommand`; on cooking failed → `CancelFoodOrderCommand` with reason; **deadline/timeout** → cancel (or documented cleanup) and saga end.
- **`KitchenService`** — success publishes `DishCookedEvent`; `KitchenException` publishes `CookingDishFailedEvent` with `getMessage()` reason. Use `Mockito.mockStatic(KitchenClient.class)` for determinism (never live `cookDish`).

Cover edge cases explicitly: kitchen failure (~20% in live client — deterministic in tests), timeout/deadline, illegal state transitions, cancel reasons, duplicate/out-of-order handling as required by the design.

### E2E / integration tests (required)

Add tests that exercise the wired flow (Spring + Axon test support or equivalent), not only isolated fixtures. At minimum:

- Happy path: create → cook success → complete.
- Failure path: create → cook failure → cancel.
- Prefer deterministic kitchen/deadline behavior in tests (do not rely on the live ~20% random failure for assertions).

Run with Maven (`./mvnw test`) before considering work done. Empty `@Test` stubs do not count as coverage.

---

## Documentation hygiene

When behavior, requirements, API, or contracts change (or when you discover gaps):

1. Update [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) for human-readable requirements / setup.
2. Update [`docs/REQUIREMENTS_CHECKLIST.md`](docs/REQUIREMENTS_CHECKLIST.md) checkboxes and summary counts.
3. Update this **`AGENTS.md`** if agent working agreements, locked surfaces, or test expectations change.

**Doc layout:** keep project docs under `docs/` except root **`README`** (if any) and this **`AGENTS.md`**.

---

## Key entrypoints & pointers

| Role | Location |
|------|----------|
| Boot | `RestaurantApplication` |
| Order aggregate | `aggregate/FoodOrderAggregate` |
| Process manager | `saga/FoodOrderSaga` |
| Cook handler | `service/KitchenService` → `KitchenClient.cookDish` |
| Order intake | `scheduler/FoodOrderScheduler` (every 15s; locked) |
| Contracts | `command/`, `event/`, `OrderState`, `Dish` |
| Axon Server UI | http://localhost:8024 |
| Axon docs (linked in Requirements) | state transitions, sagas, deadlines, aggregate/saga testing |

Suggested local loop:

```bash
docker-compose up -d
# Axon UI → Start standalone node → Complete
./mvnw test
./mvnw spring-boot:run
```

---

## Known gaps / ambiguities (treat carefully)

- Requirements say “add tests that you find valuable”; this file **requires** unit + e2e coverage including edge cases — follow the stricter bar here.
- **Invalid aggregate transitions (resolved Phase 1):** complete/cancel when not `CREATED` → `IllegalStateException`; no-prior-create → `AggregateNotFoundException`.
- **Kitchen failure reason (resolved Phase 2):** `CookingDishFailedEvent.reason` = `KitchenException.getMessage()`; kitchen unit tests use `mockStatic(KitchenClient.class)`.
- **Cooking deadline / cancel reasons (resolved Phase 3):** deadline name `"cooking-timeout"`, duration **15s**; timeout cancel reason `"Cooking timed out"`; kitchen-failure cancel propagates `CookingDishFailedEvent.reason()`.
- **E2E wiring (resolved Phase 4):** `@SpringBootTest` + profile `test`; `axon.axonserver.enabled=false`; in-memory `EventStorageEngine` (Axon Boot builds `EmbeddedEventStore`); subscribing event processors in tests; `mockStatic(KitchenClient)`; scheduling and Docker Compose off.
- **Late kitchen events after `@EndSaga` (resolved Phase 5):** saga unit fixtures prove a late `CookingDishFailedEvent` / `DishCookedEvent` after complete/cancel dispatches no further commands (`expectActiveSagas(0)`); Axon `@EndSaga` — no production guard needed.
- **Timeout/deadline path in e2e (resolved Phase 5):** `FoodOrderTimeoutE2ETest` enables JobRunr BGS for that class only, uses an async `CommandBus` so the saga lock releases while cook blocks (~20s), awaits cancel reason `"Cooking timed out"` within 25s; `@MockBean` kitchen + scheduler (static kitchen mock is thread-local).
- No custom REST API or security model is required; do not add them unless requirements change.
- Live kitchen failure is random; tests must not flaky-assert on that randomness.
