package com.nexusgroup.restaurant.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.axonframework.eventhandling.gateway.EventGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nexusgroup.restaurant.aggregate.value.Dish;
import com.nexusgroup.restaurant.command.CookDishCommand;
import com.nexusgroup.restaurant.event.CookingDishFailedEvent;
import com.nexusgroup.restaurant.event.DishCookedEvent;

@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {

    @Mock
    EventGateway eventGateway;

    KitchenService kitchenService;

    @BeforeEach
    void setUp() {
        kitchenService = new KitchenService(eventGateway);
    }

    @Test
    void handle_whenCookSucceeds_publishesDishCookedEvent() {
        try (MockedStatic<KitchenClient> kitchen = mockStatic(KitchenClient.class)) {
            kitchen.when(() -> KitchenClient.cookDish(Dish.PANCAKES)).thenAnswer(inv -> null);

            kitchenService.handle(new CookDishCommand("order-1", Dish.PANCAKES));

            verify(eventGateway).publish(new DishCookedEvent("order-1"));
            kitchen.verify(() -> KitchenClient.cookDish(Dish.PANCAKES));
        }
    }

    @Test
    void handle_whenKitchenThrows_publishesCookingDishFailedEventWithReason() {
        try (MockedStatic<KitchenClient> kitchen = mockStatic(KitchenClient.class)) {
            kitchen.when(() -> KitchenClient.cookDish(Dish.CHICKEN))
                   .thenThrow(new KitchenException("Simulated kitchen failure"));

            kitchenService.handle(new CookDishCommand("order-2", Dish.CHICKEN));

            verify(eventGateway).publish(
                new CookingDishFailedEvent("order-2", "Simulated kitchen failure"));
            verify(eventGateway, never()).publish(any(DishCookedEvent.class));
        }
    }
}
