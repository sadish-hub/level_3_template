package com.nexusgroup.restaurant.service;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Component;

import com.nexusgroup.restaurant.command.CookDishCommand;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KitchenService {

    private final EventGateway eventGateway;

    @CommandHandler
    public void handle(CookDishCommand cmd) {

    }
}
