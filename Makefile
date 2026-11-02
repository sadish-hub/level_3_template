.PHONY: help test test-unit test-e2e verify run up down clean package

help:
	@echo "Targets:"
	@echo "  help         Show this help (default)"
	@echo "  test         Run full Maven test suite"
	@echo "  test-unit    Run aggregate, saga, and kitchen unit tests"
	@echo "  test-e2e     Run Spring e2e tests (includes ~20s+ timeout test)"
	@echo "  verify       Alias for test"
	@echo "  run          Start the Spring Boot application"
	@echo "  up           Start Axon Server via Docker Compose"
	@echo "  down         Stop Docker Compose services"
	@echo "  clean        Maven clean"
	@echo "  package      Maven package (skip tests)"

test:
	mvn test

test-unit:
	mvn -Dtest=FoodOrderAggregateTest,FoodOrderSagaTest,KitchenServiceTest test

test-e2e:
	mvn -Dtest=FoodOrderFlowE2ETest,FoodOrderTimeoutE2ETest test

verify: test

run:
	mvn spring-boot:run

up:
	docker compose up -d

down:
	docker compose down

clean:
	mvn clean

package:
	mvn -DskipTests package
