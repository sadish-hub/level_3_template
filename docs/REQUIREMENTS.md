## Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Docker (for running the Axon Server)
- An IDE like IntelliJ IDEA or VS Code (optional)

## Getting Started

Run docker-compose to start Axon Server:

```bash
docker-compose up -d
```

Navigate to [http://localhost:8024](http://localhost:8024) and select `Start standalone node` in the dropdown.
Click on the `Complete` button.

Now, you can run the application using Maven:

```bash
mvn spring-boot:run
```

Alternatively, you can run the application from your IDE by running the `RestaurantApplication` class.

## Using the Application
You can interact with Axon Server via the UI available on [http://localhost:8024](http://localhost:8024).

## Useful resources
Axon State Transitions: https://docs.axoniq.io/playbook/state_transitions_via_commands/
Axon Sagas: https://docs.axoniq.io/axon-framework-reference/4.11/sagas/associations/
Axon Deadlines: https://docs.axoniq.io/axon-framework-reference/4.11/deadlines/deadline-managers/
Axon testing commands/events: https://docs.axoniq.io/axon-framework-reference/4.11/testing/commands-events/
Axon testing sagas: https://docs.axoniq.io/axon-framework-reference/4.11/testing/sagas-1/

## Agent working agreements

Agents implementing this lab should follow [`AGENTS.md`](../AGENTS.md) at the repo root for coding standards, TDD (red → green → refactor), unit + e2e testing (including edge cases), locked surfaces, and documentation hygiene. Track acceptance criteria in [`REQUIREMENTS_CHECKLIST.md`](REQUIREMENTS_CHECKLIST.md). Multi-phase implementation roadmap: [`plans/master_plan.md`](plans/master_plan.md) (detailed per-phase plans nest under `docs/plans/` later).

## Instructions
The project is using Axon Framework to orchestrate an event-sourced restaurant application.
Every 15th second, a random order is placed.

The following directories and files should not be modified:
- config
- scheduler
- command
- event
- RestaurantApplication
- service/KitchenClient
- service/KitchenException

### Level 3 - Event-Driven Order Flow
Implement an order flow using all the commands and events found in the command and event folders.
Add tests that you find valuable (see `AGENTS.md`: unit tests for aggregate/saga/kitchen **and** e2e coverage of happy path and edge cases; follow TDD).

**Aggregate rejection (Phase 1):** invalid `CompleteFoodOrderCommand` / `CancelFoodOrderCommand` when the order is not in `CREATED` throw `IllegalStateException`. Completing or cancelling with no prior create fails with Axon’s `AggregateNotFoundException`.
