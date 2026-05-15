package com.shopflow.application.service;

import java.util.UUID;

/**
 * Thrown when a {@link com.shopflow.domain.model.Customer} cannot be found
 * by the requested identifier.
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(UUID id) {
        super("Customer not found with id: " + id);
    }

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
