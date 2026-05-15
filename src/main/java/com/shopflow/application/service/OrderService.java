package com.shopflow.application.service;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.OrderStatus;

import java.util.List;
import java.util.UUID;

/**
 * Contract for order persistence and lifecycle transitions.
 *
 * <h2>Single Responsibility</h2>
 * <p>This interface covers one thing: the storage and retrieval of
 * {@link Order} aggregates, plus status transitions. It does not concern
 * itself with payment, discounts, stock, or notifications — those belong
 * to other services and are orchestrated by {@code OrderFacade}.
 */
public interface OrderService {

    /**
     * Persists a newly constructed order to the database.
     *
     * <p>The order is expected to have been built via {@code Order.Builder}
     * and to be in {@link OrderStatus#PENDING} status.
     *
     * @param order the order to save
     * @return the persisted order (with a generated {@code id})
     */
    Order save(Order order);

    /**
     * Retrieves an order by its ID, including all line items and products
     * (no N+1 queries).
     *
     * @param id the order UUID
     * @return the matching order
     * @throws OrderNotFoundException if no order exists with that id
     */
    Order findById(UUID id);

    /**
     * Returns all orders placed by a specific customer.
     *
     * @param customerId the customer's UUID
     * @return list of orders, possibly empty
     */
    List<Order> findByCustomerId(UUID customerId);

    /**
     * Returns all orders currently in the given status.
     *
     * @param status the lifecycle status to filter by
     * @return list of orders, possibly empty
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Transitions an existing order to a new status and persists the change.
     *
     * <p>All status changes go through this single method so that audit
     * logging can be added here in the future without touching callers (OCP).
     *
     * @param orderId   the order to update
     * @param newStatus the target status
     * @return the updated order
     * @throws OrderNotFoundException if no order exists with that id
     */
    Order updateStatus(UUID orderId, OrderStatus newStatus);
}
