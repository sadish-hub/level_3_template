# Requirements Checklist — Level 3 Event-Driven Order Flow

**Purpose:** Track acceptance criteria derived from `docs/REQUIREMENTS.md` against the current codebase so implementers know what is scaffolded vs still required.

**Source:** [`REQUIREMENTS.md`](REQUIREMENTS.md)  
**Agent agreements:** [`AGENTS.md`](../AGENTS.md) — coding standards, TDD, unit + e2e testing (edge cases), locked surfaces, docs hygiene.  
**Implementation roadmap:** [`plans/master_plan.md`](plans/master_plan.md) — phased goals, scope, testing focus, and checklist ID mapping. Phase 1: [`plans/phase-1-food-order-aggregate.md`](plans/phase-1-food-order-aggregate.md). Phase 2: [`plans/phase-2-kitchen-service.md`](plans/phase-2-kitchen-service.md). Phase 3: [`plans/phase-3-food-order-saga.md`](plans/phase-3-food-order-saga.md). Phase 4: [`plans/phase-4-wiring-e2e.md`](plans/phase-4-wiring-e2e.md).  
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

- [x] **FUNC-COOK-HANDLER** — `KitchenService` handles `CookDishCommand` (calls `KitchenClient.cookDish`, publishes outcome via `EventGateway`). *(Requirements → Level 3; `CookDishCommand`)*
- [x] **FUNC-COOK-SUCCESS** — Successful cook publishes `DishCookedEvent` (via `EventGateway` + `KitchenClient.cookDish`). *(Requirements → Level 3; events)*
- [x] **FUNC-COOK-FAILURE** — Kitchen failure publishes `CookingDishFailedEvent` with reason = `KitchenException.getMessage()`. *(Requirements → Level 3; `KitchenClient`)*
- [x] **FUNC-SAGA-START** — Saga reacts to `FoodOrderCreatedEvent` and dispatches `CookDishCommand` (also schedules `cooking-timeout`). *(Requirements → Level 3; Axon Sagas link)*
- [x] **FUNC-SAGA-COMPLETE** — Saga reacts to `DishCookedEvent` and dispatches `CompleteFoodOrderCommand`. *(Requirements → Level 3)*
- [x] **FUNC-SAGA-CANCEL-ON-FAIL** — Saga reacts to `CookingDishFailedEvent` and dispatches `CancelFoodOrderCommand` with `event.reason()`. *(Requirements → Level 3)*
- [x] **FUNC-DEADLINES** — Saga schedules `cooking-timeout` for 15s via `DeadlineManager`; cancels on kitchen outcome; timeout cancels order with `"Cooking timed out"`. *(Requirements → Useful resources / Level 3)*
- [x] **FUNC-ALL-COMMANDS-EVENTS** — End-to-end flow uses **all** provided commands and events: Create/Cook/Complete/Cancel + Created/DishCooked/CookingFailed/Completed/Cancelled (`FoodOrderFlowE2ETest` happy + failure). *(Requirements → Level 3)*

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
- [x] **API-NO-REST** — No custom REST API; interaction is command/event + Axon UI only (confirmed out of product scope). *(Requirements → Using the Application)*

---

## UI

- [x] **UI-AXON-DASHBOARD** — Use Axon Server dashboard only (no custom UI required by Requirements). *(Requirements → Using the Application)*

---

## Security

- [x] **SEC-NONE-SPECIFIED** — No auth/authz/TLS requirements stated; treated as out of scope. *(Requirements.md)*

---

## Non-functional

- [x] **NFR-SIMULATED-LATENCY** — Kitchen cooking times vary by dish (1s–10s) in `KitchenClient`. *(Implied by locked client; Level 3 flow)*
- [x] **NFR-SIMULATED-FAILURE** — ~20% random kitchen failure rate in `KitchenClient`. *(Implied by locked client)*
- [x] **NFR-RESILIENCE-CANCEL** — Failed or timed-out cooks result in cancelled orders (saga + cancel path; unit-proven). *(Requirements → Level 3 + Deadlines link)*

---

## Testing

- [x] **TEST-FIXTURE-AGGREGATE** — `FoodOrderAggregateTest` uses Axon `AggregateTestFixture` scaffold. *(Requirements → Level 3 “Add tests”; Axon testing commands/events link)*
- [x] **TEST-FIXTURE-SAGA** — `FoodOrderSagaTest` uses Axon `SagaTestFixture` scaffold. *(Requirements → Level 3; Axon testing sagas link)*
- [x] **TEST-TDD** — Features and bugfixes follow red → green → refactor (failing test before production/test-wiring code across Phases 1–4). *(`AGENTS.md` → TDD workflow)*
- [x] **TEST-AGGREGATE-CASES** — Meaningful aggregate unit tests for create/complete/cancel (and invalid transitions, cancel reasons). *(Requirements → Level 3; `AGENTS.md`)*
- [x] **TEST-SAGA-CASES** — Meaningful saga unit tests for created→cook+deadline, cooked→complete, kitchen failure→cancel, and timeout→cancel (`FoodOrderSagaTest`). *(Requirements → Level 3; `AGENTS.md`)*
- [x] **TEST-KITCHEN-SERVICE** — Unit coverage for cook success vs `KitchenException` → corresponding events (`KitchenServiceTest` with `mockStatic(KitchenClient)`). *(Requirements → “tests that you find valuable”; `AGENTS.md`)*
- [x] **TEST-E2E** — E2E/integration tests for wired create→cook→complete, create→cook-fail→cancel, and create→cooking-timeout→cancel (`FoodOrderTimeoutE2ETest`; JobRunr BGS; ~20s+). Deterministic kitchen stubs; in-memory Axon; no Docker. *(`AGENTS.md` → Testing expectations)*
- [x] **TEST-EDGE-CASES** — Explicit coverage of kitchen failure (unit + e2e), deadline/timeout (saga unit + timeout e2e), late events after `@EndSaga` (saga unit), invalid transitions (aggregate unit), and cancel reasons (unit + e2e). *(`AGENTS.md`)*

---

## Documentation

- [x] **DOC-AGENTS** — Root [`AGENTS.md`](../AGENTS.md) documents purpose, locked surfaces, coding standards, TDD, testing, and docs hygiene.
- [x] **DOC-SYNC** — `REQUIREMENTS.md`, this checklist, `AGENTS.md`, `README.md` / `Makefile`, and `docs/plans/` reflect Phase 5 hardening (timeout e2e, late `@EndSaga` events). *(`AGENTS.md` → Documentation hygiene)*

---

## Summary counts (as of Phase 5 hardening)

| Status | Count |
|--------|------:|
| Checked `[x]` | 41 |
| Unchecked `[ ]` | 0 |
| **Total items** | **41** |

Level 3 required checklist items closed. Phase 5 optional hardening done: timeout e2e + post-`@EndSaga` late kitchen event fixtures + Makefile/README.
