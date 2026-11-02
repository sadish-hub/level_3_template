# Requirements Checklist — Level 3 Event-Driven Order Flow

**Purpose:** Track acceptance criteria derived from `docs/REQUIREMENTS.md` against the current codebase so implementers know what is scaffolded vs still required.

**Source:** [`REQUIREMENTS.md`](REQUIREMENTS.md)  
**Agent agreements:** [`AGENTS.md`](../AGENTS.md) — coding standards, TDD, unit + e2e testing (edge cases), locked surfaces, docs hygiene.  
**Implementation roadmap:** [`plans/master_plan.md`](plans/master_plan.md) — phased goals, scope, testing focus, and checklist ID mapping. Phase 1 detailed plan: [`plans/phase-1-food-order-aggregate.md`](plans/phase-1-food-order-aggregate.md).  
**Supporting cues:** Axon playbook links in Requirements (state transitions, sagas, deadlines, aggregate/saga testing); locked command/event contracts under `command/` and `event/`.  
**Date context:** 2026-11-02 (workspace audit of template stubs).  
**Convention:** `[x]` = verified present/working in codebase today; `[ ]` = not implemented or only empty scaffold.

---

## Prerequisites & runtime environment

- [x] **PREREQ-JAVA** — Java 17+ (project targets Java 21 in `pom.xml`). *(Requirements → Prerequisites)*
- [x] **PREREQ-MAVEN** — Maven build present (`pom.xml`). *(Requirements → Prerequisites)*
- [x] **PREREQ-DOCKER-COMPOSE** — Axon Server via Docker Compose (`docker-compose.yml`). *(Requirements → Getting Started)*
- [x] **PREREQ-SPRING-COMPOSE** — Spring Boot Docker Compose integration enabled (`application.yml`). *(Requirements → Getting Started)*
- [x] **PREREQ-ENTRYPOINT** — App boots via `RestaurantApplication` with scheduling enabled. *(Requirements → Getting Started)*

---

## Constraints (do not modify)

- [x] **CONST-LOCKED-PACKAGES** — Locked areas exist and are marked DO NOT EDIT: `config/`, `scheduler/`, `command/`, `event/`, `RestaurantApplication`, `KitchenClient`, `KitchenException` (also locked value types `Dish`, `OrderState`). *(Requirements → Instructions)*

---

## Functional — scheduled order intake

- [x] **FUNC-SCHEDULE-15S** — Every 15 seconds a random `CreateFoodOrderCommand` is sent (`FoodOrderScheduler`, fixedRate 15000). *(Requirements → Instructions / Using the Application)*
- [x] **FUNC-CREATE-ORDER** — Aggregate handles `CreateFoodOrderCommand` and emits `FoodOrderCreatedEvent` (sets order id + `OrderState.CREATED`). *(Requirements → Level 3; events/commands)*
- [x] **FUNC-COMPLETE-ORDER** — Aggregate handles `CompleteFoodOrderCommand` and emits `FoodOrderCompletedEvent` (`OrderState.COMPLETED`). *(Requirements → Level 3)*
- [x] **FUNC-CANCEL-ORDER** — Aggregate handles `CancelFoodOrderCommand` and emits `FoodOrderCancelledEvent` (`OrderState.CANCELLED`). *(Requirements → Level 3)*
- [x] **FUNC-STATE-TRANSITIONS** — Aggregate enforces valid command→state transitions (Axon state-transition guidance linked in Requirements). *(Requirements → Useful resources / Level 3)*

---

## Functional — kitchen / process manager

- [ ] **FUNC-COOK-HANDLER** — `KitchenService` handles `CookDishCommand` (body currently empty). *(Requirements → Level 3; `CookDishCommand`)*
- [ ] **FUNC-COOK-SUCCESS** — Successful cook publishes `DishCookedEvent` (via `EventGateway` + `KitchenClient.cookDish`). *(Requirements → Level 3; events)*
- [ ] **FUNC-COOK-FAILURE** — Kitchen failure publishes `CookingDishFailedEvent` with reason (client throws `KitchenException` ~20% of time). *(Requirements → Level 3; `KitchenClient`)*
- [ ] **FUNC-SAGA-START** — Saga reacts to `FoodOrderCreatedEvent` and dispatches `CookDishCommand`. *(Requirements → Level 3; Axon Sagas link)*
- [ ] **FUNC-SAGA-COMPLETE** — Saga reacts to `DishCookedEvent` and dispatches `CompleteFoodOrderCommand`. *(Requirements → Level 3)*
- [ ] **FUNC-SAGA-CANCEL-ON-FAIL** — Saga reacts to `CookingDishFailedEvent` and dispatches `CancelFoodOrderCommand`. *(Requirements → Level 3)*
- [ ] **FUNC-DEADLINES** — Saga uses `DeadlineManager` for cooking timeouts / cleanup (DeadlineManager injected; no deadline API used yet; Axon Deadlines linked in Requirements). *(Requirements → Useful resources / Level 3)*
- [ ] **FUNC-ALL-COMMANDS-EVENTS** — End-to-end flow uses **all** provided commands and events: Create/Cook/Complete/Cancel + Created/DishCooked/CookingFailed/Completed/Cancelled. *(Requirements → Level 3)*

---

## Architecture / data (event sourcing)

- [x] **ARCH-AXON-SERVER** — Axon Server used for command/event routing & persistence (compose + axon starter). *(Requirements → Getting Started)*
- [x] **ARCH-JACKSON** — Jackson serializers configured for Axon (`application.yml`). *(Supporting config)*
- [x] **ARCH-AGGREGATE-SCAFFOLD** — `FoodOrderAggregate` annotated with `@Aggregate` / `@AggregateIdentifier` / `OrderState`. *(Requirements → Level 3 scaffold)*
- [x] **ARCH-EVENT-SOURCED-AGGREGATE** — Aggregate applies/replays events (`@CommandHandler` / `@EventSourcingHandler`) so state is rebuilt from the event stream. *(Requirements → Level 3; Axon event-sourced app)*
- [x] **ARCH-SAGA-SCAFFOLD** — `FoodOrderSaga` annotated with `@Saga` and wired with `CommandGateway` + `DeadlineManager`. *(Requirements → Level 3 scaffold)*
- [x] **ARCH-JOBRUNR** — JobRunr storage provider configured (supports deadline/background job infra). *(Supporting config; Deadlines resource)*

---

## API / integration surface

- [x] **API-AXON-UI** — Operators can inspect via Axon Server UI on port 8024 (documented). *(Requirements → Using the Application)*
- [x] **API-COMMAND-CONTRACTS** — Immutable command records with correct aggregate targeting annotations where required. *(Requirements → locked `command/`)*
- [x] **API-EVENT-CONTRACTS** — Immutable event records for the full order lifecycle. *(Requirements → locked `event/`)*
- [ ] **API-NO-REST** — N/A explicit REST API in Requirements; interaction is command/event + Axon UI (confirm no extra HTTP surface required). *(Gap — not specified beyond Axon UI)*

---

## UI

- [x] **UI-AXON-DASHBOARD** — Use Axon Server dashboard only (no custom UI required by Requirements). *(Requirements → Using the Application)*

---

## Security

- [ ] **SEC-NONE-SPECIFIED** — No auth/authz/TLS requirements stated; treat as out of scope unless added later. *(Gap in Requirements.md)*

---

## Non-functional

- [x] **NFR-SIMULATED-LATENCY** — Kitchen cooking times vary by dish (1s–10s) in `KitchenClient`. *(Implied by locked client; Level 3 flow)*
- [x] **NFR-SIMULATED-FAILURE** — ~20% random kitchen failure rate in `KitchenClient`. *(Implied by locked client)*
- [ ] **NFR-RESILIENCE-CANCEL** — Failed or timed-out cooks result in cancelled orders (saga + cancel path). *(Requirements → Level 3 + Deadlines link)*

---

## Testing

- [x] **TEST-FIXTURE-AGGREGATE** — `FoodOrderAggregateTest` uses Axon `AggregateTestFixture` scaffold. *(Requirements → Level 3 “Add tests”; Axon testing commands/events link)*
- [x] **TEST-FIXTURE-SAGA** — `FoodOrderSagaTest` uses Axon `SagaTestFixture` scaffold. *(Requirements → Level 3; Axon testing sagas link)*
- [ ] **TEST-TDD** — Features and bugfixes follow red → green → refactor (failing test before production code). *(`AGENTS.md` → TDD workflow)*
- [x] **TEST-AGGREGATE-CASES** — Meaningful aggregate unit tests for create/complete/cancel (and invalid transitions, cancel reasons). *(Requirements → Level 3; `AGENTS.md`)*
- [ ] **TEST-SAGA-CASES** — Meaningful saga unit tests for happy path, kitchen failure, and deadline/timeout path. *(Requirements → Level 3; `AGENTS.md`)*
- [ ] **TEST-KITCHEN-SERVICE** — Unit coverage for cook success vs `KitchenException` → corresponding events. *(Requirements → “tests that you find valuable”; `AGENTS.md`)*
- [ ] **TEST-E2E** — E2E/integration tests for wired create→cook→complete and create→cook-fail→cancel (deterministic; not flaky on live ~20% failure). *(`AGENTS.md` → Testing expectations)*
- [ ] **TEST-EDGE-CASES** — Explicit coverage of kitchen failure, deadline/timeout, invalid transitions, and cancel reasons. *(`AGENTS.md`)*

---

## Documentation

- [x] **DOC-AGENTS** — Root [`AGENTS.md`](../AGENTS.md) documents purpose, locked surfaces, coding standards, TDD, testing, and docs hygiene.
- [ ] **DOC-SYNC** — When behavior/requirements/contracts change, keep `REQUIREMENTS.md`, this checklist, and `AGENTS.md` in sync; non-README/AGENTS docs stay under `docs/`. *(`AGENTS.md` → Documentation hygiene)*

---

## Summary counts (as of Phase 1 aggregate)

| Status | Count |
|--------|------:|
| Checked `[x]` | 25 |
| Unchecked `[ ]` | 16 |
| **Total items** | **41** |

Primary remaining work: implement `FoodOrderSaga` and `KitchenService` via TDD; add e2e scenarios covering the full command/event set and edge cases; keep docs synced.
