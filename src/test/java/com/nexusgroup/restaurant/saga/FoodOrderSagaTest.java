package com.nexusgroup.restaurant.saga;

import com.nexusgroup.restaurant.aggregate.value.Dish;
import com.nexusgroup.restaurant.command.CancelFoodOrderCommand;
import com.nexusgroup.restaurant.command.CompleteFoodOrderCommand;
import com.nexusgroup.restaurant.command.CookDishCommand;
import com.nexusgroup.restaurant.event.CookingDishFailedEvent;
import com.nexusgroup.restaurant.event.DishCookedEvent;
import com.nexusgroup.restaurant.event.FoodOrderCreatedEvent;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class FoodOrderSagaTest {

    private SagaTestFixture<FoodOrderSaga> fixture;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(FoodOrderSaga.class);
    }

    @Test
    void onFoodOrderCreated_dispatchesCookDishCommand_andSchedulesCookingDeadline() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(new FoodOrderCreatedEvent("order-1", Dish.PANCAKES))
                .expectActiveSagas(1)
                .expectDispatchedCommands(new CookDishCommand("order-1", Dish.PANCAKES))
                .expectScheduledDeadlineWithName(Duration.ofSeconds(15), "cooking-timeout");
    }

    @Test
    void onDishCooked_dispatchesComplete_andEndsSaga() {
        fixture.givenAPublished(new FoodOrderCreatedEvent("order-1", Dish.CHICKEN))
                .whenPublishingA(new DishCookedEvent("order-1"))
                .expectDispatchedCommands(new CompleteFoodOrderCommand("order-1"))
                .expectActiveSagas(0)
                .expectNoScheduledDeadlines();
    }

    @Test
    void onCookingFailed_dispatchesCancelWithReason_andEndsSaga() {
        fixture.givenAPublished(new FoodOrderCreatedEvent("order-2", Dish.ICE_CREAM))
                .whenPublishingA(new CookingDishFailedEvent("order-2", "Simulated kitchen failure"))
                .expectDispatchedCommands(
                        new CancelFoodOrderCommand("order-2", "Simulated kitchen failure"))
                .expectActiveSagas(0)
                .expectNoScheduledDeadlines();
    }

    @Test
    void whenCookingDeadlineElapses_dispatchesCancelTimedOut_andEndsSaga() {
        fixture.givenAPublished(new FoodOrderCreatedEvent("order-3", Dish.BEANS_ON_TOAST))
                .whenTimeElapses(Duration.ofSeconds(15))
                .expectDispatchedCommands(
                        new CancelFoodOrderCommand("order-3", "Cooking timed out"))
                .expectActiveSagas(0);
    }

    @Test
    void afterCompleted_lateCookingFailed_dispatchesNoCommands() {
        fixture.givenAPublished(new FoodOrderCreatedEvent("order-4", Dish.CHICKEN))
                .andThenAPublished(new DishCookedEvent("order-4"))
                .whenPublishingA(new CookingDishFailedEvent("order-4", "late kitchen failure"))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    void afterCancelled_lateDishCooked_dispatchesNoCommands() {
        fixture.givenAPublished(new FoodOrderCreatedEvent("order-5", Dish.ICE_CREAM))
                .andThenAPublished(new CookingDishFailedEvent("order-5", "Simulated kitchen failure"))
                .whenPublishingA(new DishCookedEvent("order-5"))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }
}
