package com.nexusgroup.restaurant.saga;

import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FoodOrderSagaTest {

    private SagaTestFixture<FoodOrderSaga> fixture;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(FoodOrderSaga.class);
    }

    @Test
    void test() {

    }
}
