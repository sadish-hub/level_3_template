# Level 3 — Restaurant Order Lab

Event-sourced restaurant order flow (CQRS) built with Axon Framework and Spring Boot. Orders are created on a schedule, a saga drives cooking via the kitchen, and orders complete or cancel (including a cooking-timeout deadline).

## Stack

- Java 21
- Spring Boot 3.5
- Axon Framework 4.12
- JobRunr (deadline / background jobs)
- Docker Compose (Axon Server)

## Quick start

```bash
make up
```

Open [http://localhost:8024](http://localhost:8024), choose **Start standalone node**, then **Complete**.

```bash
make run
```

Every 15 seconds the locked scheduler places a random order. Inspect commands and events in the Axon Server UI.

`./mvnw` is an optional alternative to `mvn` if the wrapper is configured locally; this Makefile uses `mvn`.

## Make targets

| Target | What it does |
|--------|----------------|
| `make` / `make help` | List targets |
| `make test` | Full Maven test suite |
| `make test-unit` | Aggregate, saga, and kitchen unit tests |
| `make test-e2e` | Spring e2e (happy/fail + timeout; timeout ~20s+) |
| `make verify` | Same as `make test` |
| `make run` | `mvn spring-boot:run` |
| `make up` | Start Axon Server |
| `make down` | Stop Compose services |
| `make clean` | `mvn clean` |
| `make package` | `mvn -DskipTests package` |

## Manual testing

Operator path to exercise the live flow (not `make test`). More setup detail: [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md).

1. **Start Axon Server:** `make up` (Docker Compose).
2. **Initialize the node:** open [http://localhost:8024](http://localhost:8024) → **Start standalone node** → **Complete**.
3. **Run the app:** `make run`, or start `RestaurantApplication` from your IDE.
4. **What happens:** every 15s `FoodOrderScheduler` sends a random `CreateFoodOrderCommand`. The saga dispatches cooking via the kitchen (~1–10s; ~20% random failure). The order then completes or cancels. A **15s** cooking-timeout deadline can also cancel with reason `"Cooking timed out"`.
5. **Observe:** in the Axon Server UI, inspect commands, events, aggregates, and sagas as available. Expect **Created → Cook → Completed** or **Cancelled** (kitchen failure reason or timeout).
6. **Stop:** Ctrl+C the app; `make down` for Axon Server.

## Docs map

| Doc | Purpose |
|-----|---------|
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Product requirements and setup |
| [docs/REQUIREMENTS_CHECKLIST.md](docs/REQUIREMENTS_CHECKLIST.md) | Acceptance checklist |
| [AGENTS.md](AGENTS.md) | Agent working agreements (TDD, locked surfaces, testing) |
| [docs/plans/master_plan.md](docs/plans/master_plan.md) | Multi-phase roadmap |

## Locked surfaces

Do **not** edit `config/`, `scheduler/`, `command/`, `event/`, `RestaurantApplication`, `KitchenClient`, `KitchenException`, or `Dish` / `OrderState`. Full list: [AGENTS.md](AGENTS.md) and [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md).

## Testing notes

- Unit tests use Axon `AggregateTestFixture` / `SagaTestFixture` and Mockito for the kitchen.
- Spring e2e (`FoodOrderFlowE2ETest`) uses profile `test`, in-memory Axon (no Docker), and deterministic kitchen stubs.
- Timeout e2e (`FoodOrderTimeoutE2ETest`) enables JobRunr’s background job server for that class only, uses an async command bus so the saga lock can release while cook is in progress, and takes **~20 seconds or more**.
