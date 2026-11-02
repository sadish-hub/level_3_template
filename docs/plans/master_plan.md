# Master Plan — Level 3 Restaurant Order Flow

**Status:** Planning only — do **not** implement production restaurant logic from this document.  
**Workspace:** Level 3 Axon / Spring Boot event-sourced order lab  
**Audience:** Agents and humans implementing the lab via TDD  
**Last updated:** 2026-11-02

---

## 1. Purpose of this master plan

This document is the **roadmap** for implementing the Level 3 event-driven order flow. It expands the short phase outline into goals, scope boundaries, modules, dependencies, testing focus, edge cases, documentation updates, and exit criteria — mapped to checklist IDs in [`docs/REQUIREMENTS_CHECKLIST.md`](../REQUIREMENTS_CHECKLIST.md).

It is **not** a full per-phase implementation plan. Detailed “how to build this phase” plans are written **one phase at a time** under `docs/plans/`:

| Phase | Detailed plan file |
|------:|--------------------|
| 1 | [`docs/plans/phase-1-food-order-aggregate.md`](phase-1-food-order-aggregate.md) |
| 2 | [`docs/plans/phase-2-kitchen-service.md`](phase-2-kitchen-service.md) |
| 3 | [`docs/plans/phase-3-food-order-saga.md`](phase-3-food-order-saga.md) |
| 4 | [`docs/plans/phase-4-wiring-e2e.md`](phase-4-wiring-e2e.md) |
| 5 | [`docs/plans/phase-5-hardening.md`](phase-5-hardening.md) |

**Authoritative sources (always prefer these over this plan if they diverge):**

- Product / setup: [`docs/REQUIREMENTS.md`](../REQUIREMENTS.md)
- Acceptance tracking: [`docs/REQUIREMENTS_CHECKLIST.md`](../REQUIREMENTS_CHECKLIST.md)
- Agent working agreements (TDD, locked surfaces, testing bar): [`AGENTS.md`](../../AGENTS.md)

---

## 2. Overall architecture (start-to-end flow)

Concise runtime path using **only** the provided commands and events:

```text
FoodOrderScheduler (locked, every 15s)
  → CreateFoodOrderCommand
      → FoodOrderAggregate
          → FoodOrderCreatedEvent
              → FoodOrderSaga
                  → schedules cooking deadline (DeadlineManager)
                  → CookDishCommand
                      → KitchenService
                          → KitchenClient.cookDish (locked; latency + ~20% fail)
                          → DishCookedEvent  OR  CookingDishFailedEvent
                              → FoodOrderSaga
                                  → CompleteFoodOrderCommand  OR  CancelFoodOrderCommand
                                      → FoodOrderAggregate
                                          → FoodOrderCompletedEvent  OR  FoodOrderCancelledEvent
                                  → cancel deadline / end saga
```

**Timeout path (same saga):** if the cooking deadline fires before a kitchen outcome, saga cancels the order (documented reason) and ends.

Operators inspect the stream in Axon Server UI: http://localhost:8024

**Stack reminder:** Java 21 / Axon 4.12 / Spring Boot 3.5 / Axon Server (Docker Compose) / JobRunr + `DeadlineManager`. Package root: `com.nexusgroup.restaurant`.

---

## 3. Principles (non-negotiable for all phases)

| Principle | Expectation |
|-----------|-------------|
| **TDD** | No production behavior without a failing test first (red → verify red → green → refactor). Checklist: `TEST-TDD`. |
| **High coding standards** | Small diffs, match existing style (Lombok, Axon annotations, logging), no drive-by refactors or dependency churn. See `AGENTS.md`. |
| **Edge cases** | Explicit coverage for invalid transitions, cancel reasons, kitchen failure, deadline/timeout; deterministic tests (never flake on live ~20% failure). |
| **Unit + e2e** | Unit fixtures for aggregate / kitchen / saga; wired e2e for happy + failure paths before calling the lab done. |
| **Docs hygiene** | When behavior, contracts, or decisions change, update Requirements, checklist (and counts), and `AGENTS.md` if agreements change. Checklist: `DOC-SYNC`. |
| **Locked files** | Do **not** modify locked surfaces (see §4). Honor existing command/event shapes and `@TargetAggregateIdentifier`. |
| **No invented product features** | No REST API, security model, new commands/events, or parallel domain types unless requirements change. |
| **Layer-first order** | Build solid unit layers (aggregate → kitchen → saga), then wire + e2e. Thin vertical slice was considered and **deferred**; e2e lands in Phase 4 after units prove each layer. |

---

## 4. Locked vs editable surfaces

### Must not modify (locked — `DO NOT EDIT`)

| Area | Path / type | Checklist |
|------|-------------|-----------|
| Config | `config/` (e.g. `StorageProviderConfiguration`) | `CONST-LOCKED-PACKAGES` |
| Scheduler | `scheduler/FoodOrderScheduler` | `FUNC-SCHEDULE-15S` (already done) |
| Commands | `command/*` — Create / Cook / Complete / Cancel | `API-COMMAND-CONTRACTS` |
| Events | `event/*` — Created / DishCooked / CookingFailed / Completed / Cancelled | `API-EVENT-CONTRACTS` |
| App entry | `RestaurantApplication` | `PREREQ-ENTRYPOINT` |
| Kitchen client API | `service/KitchenClient`, `service/KitchenException` | `NFR-SIMULATED-*` |
| Value types | `aggregate/value/Dish`, `aggregate/value/OrderState` (`CREATED` \| `COMPLETED` \| `CANCELLED`) | — |

### May / should modify

| Area | Path |
|------|------|
| Aggregate | `aggregate/FoodOrderAggregate` |
| Saga | `saga/FoodOrderSaga` |
| Kitchen handler | `service/KitchenService` |
| Unit tests | `src/test/.../aggregate/FoodOrderAggregateTest`, `.../saga/FoodOrderSagaTest`, new kitchen service tests |
| E2E / integration | New under `src/test/` (none scaffolded yet) |
| Docs | `docs/*`, root `AGENTS.md`, root `README` if present |

Keep `pom.xml`, Docker Compose, and Axon wiring changes **minimal and justified**.

---

## 5. Phase overview (suggested order)

| # | Phase | Why this order | Depends on |
|--:|-------|----------------|------------|
| 1 | Food order aggregate (unit) | Domain invariants and event sourcing first — saga/e2e need a correct aggregate | — |
| 2 | Kitchen cooking path (unit) | Isolated cook → event publishing; saga needs real kitchen outcomes | Phase 1 contracts understood; kitchen independent of saga |
| 3 | Saga orchestration (unit) | Process manager wires create → cook → complete/cancel + deadlines | Phases 1–2 behaviors/events exist (fixtures can still stub commands) |
| 4 | Wiring & e2e | Prove Spring + Axon integration with deterministic doubles | Phases 1–3 unit green |
| 5 | Hardening (optional) | Extra resilience / edge polish after core acceptance | Phase 4 done |

**Why layer-first (not thin vertical slice):** fixtures catch state-transition and saga bugs cheaply; kitchen randomness and cooking latency make early e2e flaky unless doubles exist; locked scheduler/client complicate vertical slicing. Vertical confidence is Phase 4’s job.

---

## 6. Phase 1 — Food order aggregate (unit)

**Detailed plan:** [`phase-1-food-order-aggregate.md`](phase-1-food-order-aggregate.md)

### Goal / why

Implement the event-sourced `FoodOrderAggregate` so create / complete / cancel commands produce the correct events and `OrderState` transitions. Everything else assumes a trustworthy order lifecycle.

### In scope

- `@CommandHandler` for `CreateFoodOrderCommand`, `CompleteFoodOrderCommand`, `CancelFoodOrderCommand`
- Apply/replay via `@EventSourcingHandler` for `FoodOrderCreatedEvent`, `FoodOrderCompletedEvent`, `FoodOrderCancelledEvent`
- Enforce valid transitions (`CREATED` → `COMPLETED` or `CANCELLED`); reject invalid commands (do not silently no-op)
- Propagate cancel `reason` onto `FoodOrderCancelledEvent`
- Expand `FoodOrderAggregateTest` with Axon `AggregateTestFixture` (replace empty stub)

### Out of scope

- Saga, kitchen service, deadlines, Spring/e2e wiring
- Changing command/event records or `OrderState` / `Dish`
- Completing checklist items that belong to kitchen/saga/e2e

### Primary modules / files

| Role | Path |
|------|------|
| Production | `src/main/java/.../aggregate/FoodOrderAggregate.java` |
| Tests | `src/test/java/.../aggregate/FoodOrderAggregateTest.java` |
| Locked contracts (read only) | `command/Create*`, `Complete*`, `Cancel*`; `event/FoodOrder*`; `aggregate/value/OrderState`, `Dish` |

### Dependencies

None. First implementation phase.

### Testing focus (prove before moving on)

- Create → `FoodOrderCreatedEvent` (order id + dish; state becomes `CREATED`)
- Complete from `CREATED` → `FoodOrderCompletedEvent`
- Cancel from `CREATED` → `FoodOrderCancelledEvent` with reason
- Invalid transitions (e.g. complete/cancel when already completed/cancelled; complete/cancel with no prior create) fail appropriately
- Event sourcing: state rebuilds from applied events (fixture implies this)

### Key edge cases

| Case | In Phase 1? |
|------|-------------|
| Illegal complete/cancel | **Yes** |
| Cancel reason propagation | **Yes** |
| Duplicate create / out-of-order commands | Design as needed; cover if aggregate rejects; else document deferral |
| Kitchen failure / deadline | **Deferred** to Phases 2–3 |

### Docs to update when phase completes

- Check off aggregate functional + architecture + test items in `REQUIREMENTS_CHECKLIST.md`; refresh summary counts
- Note any chosen rejection semantics in Requirements or `AGENTS.md` if they clarify ambiguities
- Do **not** invent new product features in docs

### Exit criteria / definition of done

- [x] `FUNC-CREATE-ORDER` — Create → `FoodOrderCreatedEvent` + `CREATED`
- [x] `FUNC-COMPLETE-ORDER` — Complete → `FoodOrderCompletedEvent` + `COMPLETED`
- [x] `FUNC-CANCEL-ORDER` — Cancel → `FoodOrderCancelledEvent` + `CANCELLED`
- [x] `FUNC-STATE-TRANSITIONS` — Invalid transitions rejected (`IllegalStateException`)
- [x] `ARCH-EVENT-SOURCED-AGGREGATE` — Command handlers apply; ES handlers rebuild state
- [x] `TEST-AGGREGATE-CASES` — Meaningful fixture tests (create/complete/cancel, invalid transitions, cancel reason); empty stub gone
- [x] Aggregate tests green (`mvn -Dtest=FoodOrderAggregateTest test`); TDD followed (`TEST-TDD` still in progress for later phases)

---

## 7. Phase 2 — Kitchen cooking path (unit)

**Detailed plan:** [`phase-2-kitchen-service.md`](phase-2-kitchen-service.md)

### Goal / why

Implement `KitchenService` so `CookDishCommand` calls `KitchenClient.cookDish` and publishes either success or failure events. The saga depends on these outcomes; unit tests must be **deterministic** (mock/stub client behavior — never assert on live ~20% randomness).

### In scope

- `KitchenService.handle(CookDishCommand)`: call client, publish via `EventGateway`
- Success → `DishCookedEvent`
- `KitchenException` → `CookingDishFailedEvent` with reason
- New unit test class for `KitchenService` (no scaffold yet)

### Out of scope

- Saga associations, deadlines, aggregate changes (unless a tiny bugfix from Phase 1)
- Editing `KitchenClient` / `KitchenException`
- Full Spring Boot e2e

### Primary modules / files

| Role | Path |
|------|------|
| Production | `src/main/java/.../service/KitchenService.java` |
| Tests | New e.g. `src/test/java/.../service/KitchenServiceTest.java` |
| Locked (read / mock around) | `service/KitchenClient`, `service/KitchenException` |
| Contracts | `command/CookDishCommand`, `event/DishCookedEvent`, `event/CookingDishFailedEvent` |

### Dependencies

- Phase 1 ideally complete (order lifecycle stable); kitchen handler is technically independent of aggregate code
- Understanding of cook command/event contracts

### Testing focus

- Happy cook publishes `DishCookedEvent` with correct `orderId`
- Forced `KitchenException` publishes `CookingDishFailedEvent` with reason (e.g. message from exception)
- No reliance on `ThreadLocalRandom` failure rate or real `Thread.sleep` cooking delays in assertions (inject/mock client or wrap call site for tests)

### Key edge cases

| Case | In Phase 2? |
|------|-------------|
| Deterministic success vs failure | **Yes** |
| Reason string on failure event | **Yes** (align with client message or documented mapping) |
| Saga reaction to kitchen events | **Deferred** to Phase 3 |
| Cooking latency / JobRunr | **Deferred** (NFR already true in locked client) |

### Docs to update when phase completes

- Checklist: kitchen functional + `TEST-KITCHEN-SERVICE`
- Document any chosen failure reason mapping if not identical to `KitchenException` message

### Exit criteria / definition of done

- [x] `FUNC-COOK-HANDLER` — Handler implemented (not empty)
- [x] `FUNC-COOK-SUCCESS` — Success → `DishCookedEvent` via `EventGateway`
- [x] `FUNC-COOK-FAILURE` — `KitchenException` → `CookingDishFailedEvent` with reason (`ex.getMessage()`)
- [x] `TEST-KITCHEN-SERVICE` — Unit tests for success and failure paths (`mockStatic`)
- [x] Tests deterministic; `mvn test` green for kitchen + prior phases

---

## 8. Phase 3 — Saga orchestration (unit)

**Detailed plan:** [`phase-3-food-order-saga.md`](phase-3-food-order-saga.md)

### Goal / why

Implement `FoodOrderSaga` as the process manager: on order created, start cooking (and a deadline); on kitchen success/failure or timeout, complete or cancel and end the saga. This is the orchestration glue between aggregate and kitchen **without** requiring full Spring Boot yet (`SagaTestFixture`).

### In scope

- Associate saga on order id
- On `FoodOrderCreatedEvent` → dispatch `CookDishCommand`; schedule cooking deadline via `DeadlineManager`
- On `DishCookedEvent` → `CompleteFoodOrderCommand`; cancel deadline; end saga
- On `CookingDishFailedEvent` → `CancelFoodOrderCommand` with reason; cancel deadline; end saga
- On deadline → cancel (documented reason) and/or cleanup; end saga
- Keep `commandGateway` / `deadlineManager` fields `transient` as scaffolded
- Expand `FoodOrderSagaTest` (replace empty stub)

### Out of scope

- Changing locked commands/events
- Live Axon Server e2e (Phase 4)
- Optional hardening beyond core deadline cancel path (Phase 5)

### Primary modules / files

| Role | Path |
|------|------|
| Production | `src/main/java/.../saga/FoodOrderSaga.java` |
| Tests | `src/test/java/.../saga/FoodOrderSagaTest.java` |
| Injected (scaffold) | `CommandGateway`, `DeadlineManager` |

### Dependencies

- Phase 1: aggregate accepts complete/cancel (saga will dispatch those commands in real runs)
- Phase 2: kitchen publishes the events the saga listens for
- Fixture tests can still drive events/commands without Spring

### Testing focus

- Created → expects `CookDishCommand` (+ deadline scheduled)
- Cooked → expects `CompleteFoodOrderCommand`; saga ends; deadline cancelled
- Cooking failed → expects `CancelFoodOrderCommand` with reason; saga ends
- Deadline fires → cancel (or documented cleanup) + saga end
- Association keyed by order id (no cross-talk between orders in fixture scenarios)

### Key edge cases

| Case | In Phase 3? |
|------|-------------|
| Timeout / deadline cancel reason | **Yes** — choose clear string; document in `docs/` |
| Kitchen failure cancel reason | **Yes** — propagate or map from event reason |
| Event after saga already ended | Prefer ignore/safe; cover if design requires |
| Duplicate kitchen outcomes | Design + test or defer to Phase 5 with note |
| Exact deadline duration | **Resolved Phase 3** — `"cooking-timeout"`, 15s; timeout reason `"Cooking timed out"` |

### Docs to update when phase completes

- Checklist: saga + deadlines + resilience cancel items as met at unit level
- Document **deadline duration** and **cancel reason strings** (timeout vs kitchen failure) in Requirements and/or checklist notes / `AGENTS.md` known gaps section

### Exit criteria / definition of done

- [x] `FUNC-SAGA-START` — Created → `CookDishCommand`
- [x] `FUNC-SAGA-COMPLETE` — Dish cooked → `CompleteFoodOrderCommand`
- [x] `FUNC-SAGA-CANCEL-ON-FAIL` — Cooking failed → `CancelFoodOrderCommand`
- [x] `FUNC-DEADLINES` — `DeadlineManager` used for timeout/cleanup (`cooking-timeout`, 15s)
- [x] `NFR-RESILIENCE-CANCEL` — Failed/timed-out cooks lead to cancel path (unit-proven)
- [x] `TEST-SAGA-CASES` — Happy, failure, deadline paths; empty stub gone
- [x] Timeout edge covered under `TEST-SAGA-CASES`; full `TEST-EDGE-CASES` left for Phase 4; `mvn test` green

---

## 9. Phase 4 — Wiring & e2e

**Detailed plan:** [`phase-4-wiring-e2e.md`](phase-4-wiring-e2e.md)

### Goal / why

Prove the **wired** flow under Spring + Axon test support (or equivalent): create → cook success → complete, and create → cook failure → cancel. Catch integration gaps that fixtures miss (bean wiring, serializers, command/event routing).

### In scope

- E2E/integration tests under `src/test/` (new; none scaffolded)
- Deterministic kitchen and/or deadline behavior in tests (override/mock — do not flake on ~20% live failure or long sleeps)
- Minimal wiring fixes only if units pass but Spring context fails (justify any `pom` / config touch)
- Confirm end-to-end use of all provided commands and events
- Confirm no custom REST/security added (`API-NO-REST`, `SEC-NONE-SPECIFIED` remain out of product scope)

### Out of scope

- Optional polish listed in Phase 5
- Changing locked scheduler/client for production behavior (test doubles only)
- Implementing missing unit behavior that should have landed in Phases 1–3 (fix forward if gaps appear)

### Primary modules / files

| Role | Path |
|------|------|
| New tests | e.g. `src/test/java/.../` integration/e2e package (name TBD in phase plan) |
| Production under test | Aggregate + Saga + `KitchenService` as wired by Spring Boot / Axon |
| Infra (prefer not to change) | `docker-compose.yml`, `application.yml`, Axon Server |

### Dependencies

Phases 1–3 unit-complete and green.

### Testing focus

- Happy path: create → cook success → complete
- Failure path: create → cook failure → cancel
- Deterministic doubles for kitchen (and deadline if asserting timeout in e2e)
- Prefer not requiring a manually started Axon UI for CI; use test support / Testcontainers / embedded approach as justified in the later phase plan
- Full `./mvnw test` suite green

### Key edge cases

| Case | In Phase 4? |
|------|-------------|
| Wired happy + failure | **Yes** |
| Timeout e2e | Optional here if slow; at least unit-covered in Phase 3 — document if e2e deferred to Phase 5 |
| Flaky random kitchen | **Must avoid** |
| Invalid transition under load | Prefer unit coverage; e2e only if valuable |

### Docs to update when phase completes

- Checklist: `FUNC-ALL-COMMANDS-EVENTS`, `TEST-E2E`, remaining `TEST-EDGE-CASES`, `TEST-TDD`, `DOC-SYNC` as appropriate
- Refresh summary counts; note how e2e achieves determinism

### Exit criteria / definition of done

- [x] `FUNC-ALL-COMMANDS-EVENTS` — Full create/cook/complete/cancel + all five event types exercised in the flow
- [x] `TEST-E2E` — Happy and failure integration tests exist and pass
- [x] `TEST-EDGE-CASES` — Kitchen failure, deadline (unit), invalid transitions, cancel reasons covered across suite
- [x] `TEST-TDD` — Workflow followed for features in this phase
- [x] `DOC-SYNC` — Docs/checklist reflect shipped behavior
- [x] `mvn test` fully green; empty `@Test` stubs eliminated; timeout e2e deferred to Phase 5

---

## 10. Phase 5 — Hardening (optional)

**Detailed plan:** [`phase-5-hardening.md`](phase-5-hardening.md)

### Goal / why

After core acceptance, optionally strengthen resilience and clarity: duplicate/out-of-order handling, timeout e2e if deferred, logging consistency, checklist/docs polish. **Skip if Phase 4 already meets the bar** in `AGENTS.md` / checklist.

### In scope (examples — only if needed)

- Explicit handling of late kitchen events after cancel/complete
- Stronger deadline cleanup guarantees
- Timeout path in e2e if not done in Phase 4
- Doc clarifications for operators (deadline duration, reason strings)
- Minor refactors while staying green (no new untested behavior)

### Out of scope

- New product features, REST, security, new command/event types
- Unlocking or rewriting locked packages

### Primary modules / files

Same editable surfaces as Phases 1–4; docs under `docs/`.

### Dependencies

Phase 4 complete (or explicitly waived with checklist justification).

### Testing focus

Regression of full unit + e2e suite; targeted tests for any new edge behavior.

### Key edge cases

Duplicate commands, out-of-order kitchen vs deadline races, double-cancel attempts — cover what the design claims to handle.

### Docs to update when phase completes

- Checklist remaining gaps; `AGENTS.md` known-gaps if decisions close them
- Summary counts

### Exit criteria / definition of done

- [x] No open **required** checklist items for Level 3 flow
- [x] Optional items either done or explicitly marked deferred/out of scope in docs (timeout e2e + late `@EndSaga` events done; logging polish skipped/YAGNI)
- [x] `mvn test` / `make test` green

---

## 11. Risks & open decisions

| Topic | Risk / decision | Guidance |
|-------|-----------------|----------|
| **Cooking deadline duration** | Resolved Phase 3 | `"cooking-timeout"`, `Duration.ofSeconds(15)` (> `CHICKEN` 10s + buffer) |
| **Cancel reason strings** | Resolved Phase 3 | Timeout: `"Cooking timed out"`; kitchen failure: propagate `CookingDishFailedEvent.reason()` |
| **Deterministic kitchen in tests** | Live client is random + blocking sleep | Mock/stub in unit and e2e; never flake on ~20% failure |
| **Java version mismatch** | Requirements say Java 17+; `pom.xml` targets **21** | Develop/test on JDK 21; treat 17+ as minimum floor in prose only |
| **Axon Server for e2e** | UI requires manual “Start standalone node” | Prefer automated test approach; don’t make CI depend on manual UI clicks |
| **Layer-first delay of e2e** | Integration bugs found late | Mitigate by keeping phases thin and starting Phase 4 as soon as units are green |
| **JobRunr / DeadlineManager wiring** | Scaffold + config present; unused until Phase 3 | Touch config only if deadlines fail to fire in real context — justify in phase plan |
| **Strictness vs Requirements wording** | Requirements say “tests you find valuable”; `AGENTS.md` requires unit + e2e + edges | Follow the **stricter** `AGENTS.md` bar |
| **Scope creep** | Temptation to add REST/read models/UI | Out of scope unless requirements change (`API-NO-REST`, `SEC-NONE-SPECIFIED`) |

---

## 12. How to use this plan next

1. **Do not implement code from this master plan alone.**
2. Write a **detailed** implementation plan for the next phase only: TDD steps, concrete test names, expected events, open decisions resolved for that phase. Phase 1 plan: [`phase-1-food-order-aggregate.md`](phase-1-food-order-aggregate.md).
3. Execute that phase plan (red → green → refactor).
4. Update checklist + docs; then author the next phase plan.
5. Repeat through Phase 4 (and Phase 5 only if needed).

---

## 13. Quick reference — unchecked checklist IDs by phase

| Phase | Primary checklist IDs to close |
|------:|--------------------------------|
| 1 | `FUNC-CREATE-ORDER`, `FUNC-COMPLETE-ORDER`, `FUNC-CANCEL-ORDER`, `FUNC-STATE-TRANSITIONS`, `ARCH-EVENT-SOURCED-AGGREGATE`, `TEST-AGGREGATE-CASES` |
| 2 | `FUNC-COOK-HANDLER`, `FUNC-COOK-SUCCESS`, `FUNC-COOK-FAILURE`, `TEST-KITCHEN-SERVICE` |
| 3 | `FUNC-SAGA-START`, `FUNC-SAGA-COMPLETE`, `FUNC-SAGA-CANCEL-ON-FAIL`, `FUNC-DEADLINES`, `NFR-RESILIENCE-CANCEL`, `TEST-SAGA-CASES` (+ edge subset) |
| 4 | `FUNC-ALL-COMMANDS-EVENTS`, `TEST-E2E`, `TEST-EDGE-CASES`, `TEST-TDD`, `DOC-SYNC` |
| 5 | Remaining polish / deferred edges; confirm all required `[ ]` closed |

Already satisfied scaffolding (do not re-do): prereqs, locked packages, scheduler, Axon/Jackson/JobRunr scaffolds, aggregate/saga annotations, fixture classes present, command/event contracts, Axon UI docs.
