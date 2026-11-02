package com.nexusgroup.restaurant.e2e;

import org.axonframework.config.ConfigurerModule;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * In-memory EventStore for Spring e2e tests (no Axon Server).
 * Only {@link EventStorageEngine} is overridden; Axon Boot creates the
 * {@code EmbeddedEventStore}. Subscribing processors keep saga + recorder
 * handling synchronous and deterministic.
 */
@TestConfiguration
public class AxonInMemoryTestConfig {

    @Bean
    @Primary
    public EventStorageEngine eventStorageEngine() {
        return new InMemoryEventStorageEngine();
    }

    @Bean
    public ConfigurerModule subscribingProcessorsConfigurerModule() {
        return configurer -> configurer.eventProcessing(
            EventProcessingConfigurer::usingSubscribingEventProcessors);
    }
}
