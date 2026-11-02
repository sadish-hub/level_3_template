package com.nexusgroup.restaurant.e2e;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import com.nexusgroup.restaurant.event.CookingDishFailedEvent;
import com.nexusgroup.restaurant.event.DishCookedEvent;
import com.nexusgroup.restaurant.event.FoodOrderCancelledEvent;
import com.nexusgroup.restaurant.event.FoodOrderCompletedEvent;
import com.nexusgroup.restaurant.event.FoodOrderCreatedEvent;

@Component
public class OrderFlowEventRecorder {

    private final List<FoodOrderCreatedEvent> created = new CopyOnWriteArrayList<>();
    private final List<DishCookedEvent> cooked = new CopyOnWriteArrayList<>();
    private final List<CookingDishFailedEvent> cookingFailed = new CopyOnWriteArrayList<>();
    private final List<FoodOrderCompletedEvent> completed = new CopyOnWriteArrayList<>();
    private final List<FoodOrderCancelledEvent> cancelled = new CopyOnWriteArrayList<>();

    @EventHandler
    public void on(FoodOrderCreatedEvent event) {
        created.add(event);
    }

    @EventHandler
    public void on(DishCookedEvent event) {
        cooked.add(event);
    }

    @EventHandler
    public void on(CookingDishFailedEvent event) {
        cookingFailed.add(event);
    }

    @EventHandler
    public void on(FoodOrderCompletedEvent event) {
        completed.add(event);
    }

    @EventHandler
    public void on(FoodOrderCancelledEvent event) {
        cancelled.add(event);
    }

    public void reset() {
        created.clear();
        cooked.clear();
        cookingFailed.clear();
        completed.clear();
        cancelled.clear();
    }

    public List<FoodOrderCreatedEvent> created() {
        return List.copyOf(created);
    }

    public List<DishCookedEvent> cooked() {
        return List.copyOf(cooked);
    }

    public List<CookingDishFailedEvent> cookingFailed() {
        return List.copyOf(cookingFailed);
    }

    public List<FoodOrderCompletedEvent> completed() {
        return List.copyOf(completed);
    }

    public List<FoodOrderCancelledEvent> cancelled() {
        return List.copyOf(cancelled);
    }
}
