package com.nexusgroup.restaurant.saga;

import com.nexusgroup.restaurant.command.CancelFoodOrderCommand;
import com.nexusgroup.restaurant.command.CompleteFoodOrderCommand;
import com.nexusgroup.restaurant.command.CookDishCommand;
import com.nexusgroup.restaurant.event.CookingDishFailedEvent;
import com.nexusgroup.restaurant.event.DishCookedEvent;
import com.nexusgroup.restaurant.event.FoodOrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.time.Duration;

@Saga
@Slf4j
public class FoodOrderSaga implements Serializable {

    static final String COOKING_DEADLINE = "cooking-timeout";
    static final Duration COOKING_TIMEOUT = Duration.ofSeconds(15);
    static final String TIMEOUT_REASON = "Cooking timed out";

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

    private String orderId;
    private String cookingDeadlineId;

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(FoodOrderCreatedEvent event) {
        orderId = event.orderId();
        cookingDeadlineId = deadlineManager.schedule(COOKING_TIMEOUT, COOKING_DEADLINE);
        commandGateway.send(new CookDishCommand(event.orderId(), event.dish()));
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(DishCookedEvent event) {
        cancelCookingDeadline();
        commandGateway.send(new CompleteFoodOrderCommand(event.orderId()));
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void on(CookingDishFailedEvent event) {
        cancelCookingDeadline();
        commandGateway.send(new CancelFoodOrderCommand(event.orderId(), event.reason()));
    }

    @EndSaga
    @DeadlineHandler(deadlineName = COOKING_DEADLINE)
    public void onCookingTimeout() {
        commandGateway.send(new CancelFoodOrderCommand(orderId, TIMEOUT_REASON));
    }

    private void cancelCookingDeadline() {
        if (cookingDeadlineId != null) {
            deadlineManager.cancelSchedule(COOKING_DEADLINE, cookingDeadlineId);
            cookingDeadlineId = null;
        }
    }

}
