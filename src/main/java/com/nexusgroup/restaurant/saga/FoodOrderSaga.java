package com.nexusgroup.restaurant.saga;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;

@Saga
@Slf4j
public class FoodOrderSaga implements Serializable {

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

}
