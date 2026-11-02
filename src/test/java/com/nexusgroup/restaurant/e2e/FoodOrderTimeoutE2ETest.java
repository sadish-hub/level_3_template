package com.nexusgroup.restaurant.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.Duration;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.nexusgroup.restaurant.aggregate.value.Dish;
import com.nexusgroup.restaurant.command.CookDishCommand;
import com.nexusgroup.restaurant.command.CreateFoodOrderCommand;
import com.nexusgroup.restaurant.scheduler.FoodOrderScheduler;
import com.nexusgroup.restaurant.service.KitchenService;

@SpringBootTest(properties = {
        "jobrunr.background-job-server.enabled=true",
        "spring.task.scheduling.enabled=false"
})
@ActiveProfiles("test")
@Import({AxonInMemoryTestConfig.class, TimeoutE2ECommandBusConfig.class})
class FoodOrderTimeoutE2ETest {

    @Autowired
    CommandGateway commandGateway;

    @Autowired
    OrderFlowEventRecorder recorder;

    /**
     * Replaces the real kitchen so cook can block without {@code mockStatic}
     * (thread-local) failing on the async command-bus thread.
     */
    @MockBean
    KitchenService kitchenService;

    /**
     * {@code @EnableScheduling} on the app still registers scheduled methods even
     * when {@code spring.task.scheduling.enabled=false}; neutralize random orders.
     */
    @MockBean
    FoodOrderScheduler foodOrderScheduler;

    @Test
    void createOrder_whenCookingExceedsDeadline_cancelsWithTimeoutReason() {
        recorder.reset();
        String orderId = "e2e-timeout-1";

        doAnswer(inv -> {
            Thread.sleep(20_000);
            return null;
        }).when(kitchenService).handle(any(CookDishCommand.class));

        commandGateway.send(new CreateFoodOrderCommand(orderId, Dish.PANCAKES));

        await().atMost(Duration.ofSeconds(25)).untilAsserted(() -> {
            assertThat(recorder.created()).anyMatch(e -> orderId.equals(e.orderId()));
            assertThat(recorder.cancelled())
                .anyMatch(e -> orderId.equals(e.orderId())
                    && "Cooking timed out".equals(e.reason()));
            assertThat(recorder.completed()).noneMatch(e -> orderId.equals(e.orderId()));
            assertThat(recorder.cookingFailed()).noneMatch(e -> orderId.equals(e.orderId()));
            assertThat(recorder.cooked()).noneMatch(e -> orderId.equals(e.orderId()));
        });
    }
}
