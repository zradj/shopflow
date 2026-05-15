package com.shopflow.application.service;

import com.shopflow.api.dto.CreateCustomerRequest;
import com.shopflow.domain.model.Customer;

import java.util.List;
import java.util.UUID;

/**
 * Application-level contract for customer management.
 *
 * <p>Controllers depend only on this interface, never on a concrete class,
 * keeping the HTTP layer decoupled from persistence details (OCP).
 */
public interface CustomerService {

    /**
     * Returns every registered customer.
     */
    List<Customer> findAll();

    /**
     * Retrieves a single customer by their unique identifier.
     *
     * @param id the customer UUID
     * @return the matching customer
     * @throws CustomerNotFoundException if no customer exists with that id
     */
    Customer findById(UUID id);

    /**
     * Creates and persists a new customer.
     *
     * @param request the validated creation request
     * @return the newly created customer
     * @throws IllegalArgumentException if the email address is already in use
     */
    Customer create(CreateCustomerRequest request);
}
