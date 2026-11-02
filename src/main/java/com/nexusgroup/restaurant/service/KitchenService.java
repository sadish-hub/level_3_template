package com.nexusgroup.restaurant.service;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Component;

import com.nexusgroup.restaurant.command.CookDishCommand;
import com.nexusgroup.restaurant.event.CookingDishFailedEvent;
import com.nexusgroup.restaurant.event.DishCookedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KitchenService {

    private final EventGateway eventGateway;

    @CommandHandler
    public void handle(CookDishCommand cmd) {
        try {
            KitchenClient.cookDish(cmd.dish());
            eventGateway.publish(new DishCookedEvent(cmd.orderId()));
        } catch (KitchenException ex) {
            eventGateway.publish(new CookingDishFailedEvent(cmd.orderId(), ex.getMessage()));
        }
    }
}
