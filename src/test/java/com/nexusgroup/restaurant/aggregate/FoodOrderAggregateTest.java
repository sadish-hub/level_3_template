package com.nexusgroup.restaurant.aggregate;

import com.nexusgroup.restaurant.aggregate.value.Dish;
import com.nexusgroup.restaurant.command.CancelFoodOrderCommand;
import com.nexusgroup.restaurant.command.CompleteFoodOrderCommand;
import com.nexusgroup.restaurant.command.CreateFoodOrderCommand;
import com.nexusgroup.restaurant.event.FoodOrderCancelledEvent;
import com.nexusgroup.restaurant.event.FoodOrderCompletedEvent;
import com.nexusgroup.restaurant.event.FoodOrderCreatedEvent;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FoodOrderAggregateTest {

    private FixtureConfiguration<FoodOrderAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(FoodOrderAggregate.class);
    }

    @Test
    void createsFoodOrder_emitsCreatedEvent() {
        fixture.givenNoPriorActivity()
               .when(new CreateFoodOrderCommand("order-1", Dish.PANCAKES))
               .expectSuccessfulHandlerExecution()
               .expectEvents(new FoodOrderCreatedEvent("order-1", Dish.PANCAKES));
    }

    @Test
    void completesFoodOrder_whenCreated_emitsCompletedEvent() {
        fixture.given(new FoodOrderCreatedEvent("order-1", Dish.CHICKEN))
               .when(new CompleteFoodOrderCommand("order-1"))
               .expectSuccessfulHandlerExecution()
               .expectEvents(new FoodOrderCompletedEvent("order-1"));
    }

    @Test
    void cancelsFoodOrder_whenCreated_emitsCancelledEventWithReason() {
        fixture.given(new FoodOrderCreatedEvent("order-1", Dish.ICE_CREAM))
               .when(new CancelFoodOrderCommand("order-1", "customer changed mind"))
               .expectSuccessfulHandlerExecution()
               .expectEvents(new FoodOrderCancelledEvent("order-1", "customer changed mind"));
    }

    @Test
    void complete_whenAlreadyCompleted_throwsIllegalStateException() {
        fixture.given(
                       new FoodOrderCreatedEvent("order-1", Dish.PANCAKES),
                       new FoodOrderCompletedEvent("order-1"))
               .when(new CompleteFoodOrderCommand("order-1"))
               .expectException(IllegalStateException.class);
    }

    @Test
    void cancel_whenAlreadyCompleted_throwsIllegalStateException() {
        fixture.given(
                       new FoodOrderCreatedEvent("order-1", Dish.PANCAKES),
                       new FoodOrderCompletedEvent("order-1"))
               .when(new CancelFoodOrderCommand("order-1", "too late"))
               .expectException(IllegalStateException.class);
    }

    @Test
    void complete_whenAlreadyCancelled_throwsIllegalStateException() {
        fixture.given(
                       new FoodOrderCreatedEvent("order-1", Dish.BEANS_ON_TOAST),
                       new FoodOrderCancelledEvent("order-1", "kitchen closed"))
               .when(new CompleteFoodOrderCommand("order-1"))
               .expectException(IllegalStateException.class);
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsIllegalStateException() {
        fixture.given(
                       new FoodOrderCreatedEvent("order-1", Dish.BEANS_ON_TOAST),
                       new FoodOrderCancelledEvent("order-1", "kitchen closed"))
               .when(new CancelFoodOrderCommand("order-1", "again"))
               .expectException(IllegalStateException.class);
    }

    @Test
    void complete_withNoPriorCreate_fails() {
        fixture.givenNoPriorActivity()
               .when(new CompleteFoodOrderCommand("order-1"))
               .expectException(AggregateNotFoundException.class);
    }

    @Test
    void cancel_withNoPriorCreate_fails() {
        fixture.givenNoPriorActivity()
               .when(new CancelFoodOrderCommand("order-1", "no such order"))
               .expectException(AggregateNotFoundException.class);
    }
}
