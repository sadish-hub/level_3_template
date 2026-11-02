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
make docker-up
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
| `make docker-up` | Start Axon Server |
| `make docker-down` | Stop Compose services |
| `make clean` | `mvn clean` |
| `make package` | `mvn -DskipTests package` |

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
