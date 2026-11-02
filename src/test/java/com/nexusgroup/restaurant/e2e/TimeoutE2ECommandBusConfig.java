package com.nexusgroup.restaurant.e2e;

import org.axonframework.commandhandling.AsynchronousCommandBus;
import org.axonframework.commandhandling.CommandBus;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Async command dispatch so the saga can finish {@code FoodOrderCreatedEvent}
 * handling (and release the saga lock) while a slow cook is still in progress —
 * required for JobRunr {@code cooking-timeout} to fire in e2e.
 */
@TestConfiguration
public class TimeoutE2ECommandBusConfig {

    @Bean
    @Primary
    public CommandBus asynchronousCommandBus() {
        return AsynchronousCommandBus.builder().build();
    }
}
