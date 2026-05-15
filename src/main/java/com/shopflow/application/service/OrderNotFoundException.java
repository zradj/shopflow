package com.shopflow.application.service;

import java.util.UUID;

/**
 * Thrown by {@link OrderService} when an order cannot be found by the
 * requested identifier.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Order not found with id: " + id);
    }
}
