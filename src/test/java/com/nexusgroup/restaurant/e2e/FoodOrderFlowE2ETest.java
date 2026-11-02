package com.nexusgroup.restaurant.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.time.Duration;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.nexusgroup.restaurant.aggregate.value.Dish;
import com.nexusgroup.restaurant.command.CreateFoodOrderCommand;
import com.nexusgroup.restaurant.scheduler.FoodOrderScheduler;
import com.nexusgroup.restaurant.service.KitchenClient;
import com.nexusgroup.restaurant.service.KitchenException;

@SpringBootTest
@ActiveProfiles("test")
@Import(AxonInMemoryTestConfig.class)
class FoodOrderFlowE2ETest {

    @Autowired
    CommandGateway commandGateway;

    @Autowired
    OrderFlowEventRecorder recorder;

    /**
     * {@code @EnableScheduling} on the app still registers scheduled methods even
     * when {@code spring.task.scheduling.enabled=false}; neutralize random orders.
     */
    @MockBean
    FoodOrderScheduler foodOrderScheduler;

    @Test
    void createOrder_whenKitchenSucceeds_completesOrder() {
        recorder.reset();
        String orderId = "e2e-success-1";
        try (MockedStatic<KitchenClient> kitchen = mockStatic(KitchenClient.class)) {
            kitchen.when(() -> KitchenClient.cookDish(any())).thenAnswer(inv -> null);

            commandGateway.sendAndWait(new CreateFoodOrderCommand(orderId, Dish.PANCAKES));

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertThat(recorder.created()).anyMatch(e -> orderId.equals(e.orderId()));
                assertThat(recorder.cooked()).anyMatch(e -> orderId.equals(e.orderId()));
                assertThat(recorder.completed()).anyMatch(e -> orderId.equals(e.orderId()));
                assertThat(recorder.cancelled()).noneMatch(e -> orderId.equals(e.orderId()));
            });
        }
    }

    @Test
    void createOrder_whenKitchenFails_cancelsOrderWithReason() {
        recorder.reset();
        String orderId = "e2e-fail-1";
        String reason = "Simulated kitchen failure";
        try (MockedStatic<KitchenClient> kitchen = mockStatic(KitchenClient.class)) {
            kitchen.when(() -> KitchenClient.cookDish(any()))
                   .thenThrow(new KitchenException(reason));

            commandGateway.sendAndWait(new CreateFoodOrderCommand(orderId, Dish.CHICKEN));

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertThat(recorder.created()).anyMatch(e -> orderId.equals(e.orderId()));
                assertThat(recorder.cookingFailed())
                    .anyMatch(e -> orderId.equals(e.orderId()) && reason.equals(e.reason()));
                assertThat(recorder.cancelled())
                    .anyMatch(e -> orderId.equals(e.orderId()) && reason.equals(e.reason()));
                assertThat(recorder.completed()).noneMatch(e -> orderId.equals(e.orderId()));
            });
        }
    }
}
