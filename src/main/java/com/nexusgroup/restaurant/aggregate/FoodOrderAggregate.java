package com.nexusgroup.restaurant.aggregate;

import com.nexusgroup.restaurant.aggregate.value.OrderState;
import com.nexusgroup.restaurant.command.CancelFoodOrderCommand;
import com.nexusgroup.restaurant.command.CompleteFoodOrderCommand;
import com.nexusgroup.restaurant.command.CreateFoodOrderCommand;
import com.nexusgroup.restaurant.event.FoodOrderCancelledEvent;
import com.nexusgroup.restaurant.event.FoodOrderCompletedEvent;
import com.nexusgroup.restaurant.event.FoodOrderCreatedEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.io.Serializable;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Slf4j
@Aggregate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodOrderAggregate implements Serializable {

    @AggregateIdentifier
    private String orderId;

    private OrderState state;

    @CommandHandler
    public FoodOrderAggregate(CreateFoodOrderCommand command) {
        apply(new FoodOrderCreatedEvent(command.orderId(), command.dish()));
    }

    @CommandHandler
    public void handle(CompleteFoodOrderCommand command) {
        if (state != OrderState.CREATED) {
            throw new IllegalStateException("Cannot complete food order in state: " + state);
        }
        apply(new FoodOrderCompletedEvent(command.orderId()));
    }

    @CommandHandler
    public void handle(CancelFoodOrderCommand command) {
        if (state != OrderState.CREATED) {
            throw new IllegalStateException("Cannot cancel food order in state: " + state);
        }
        apply(new FoodOrderCancelledEvent(command.orderId(), command.reason()));
    }

    @EventSourcingHandler
    public void on(FoodOrderCreatedEvent event) {
        this.orderId = event.orderId();
        this.state = OrderState.CREATED;
    }

    @EventSourcingHandler
    public void on(FoodOrderCompletedEvent event) {
        this.state = OrderState.COMPLETED;
    }

    @EventSourcingHandler
    public void on(FoodOrderCancelledEvent event) {
        this.state = OrderState.CANCELLED;
    }

}
