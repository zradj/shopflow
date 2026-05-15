package com.shopflow.application.service;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.Product;

import java.util.UUID;

/**
 * Contract for inventory operations: stock validation, deduction, and
 * manual adjustments.
 *
 * <h2>Single Responsibility</h2>
 * <p>All stock-mutation logic lives here and nowhere else. {@code OrderService}
 * persists orders; {@code InventoryService} guards and adjusts stock levels.
 * The two never cross into each other's domain.
 *
 * <h2>Cache coupling</h2>
 * <p>Implementations are expected to evict the Redis product cache after any
 * mutation so that {@code CachingProductService} does not serve stale stock
 * counts to subsequent readers.
 */
public interface InventoryService {

    /**
     * Checks whether every line item in the order can be fulfilled by current
     * stock levels, without modifying any data.
     *
     * @param order the order whose items will be validated
     * @throws InsufficientStockException if any item cannot be fulfilled
     */
    void validateStock(Order order);

    /**
     * Atomically deducts stock for every line item in the order.
     * Should only be called after {@link #validateStock} has passed.
     *
     * @param order the confirmed order whose stock should be deducted
     * @throws InsufficientStockException if a race condition has depleted
     *                                    stock between validation and deduction
     */
    void deductStock(Order order);

    /**
     * Manually adjusts the stock of a single product — used by the
     * {@code InventoryController} for warehouse top-ups or corrections.
     *
     * @param productId the product to update
     * @param quantity  the new absolute stock quantity (must be ≥ 0)
     * @return the updated {@link Product}
     * @throws ProductNotFoundException if no product exists with that id
     * @throws IllegalArgumentException if quantity is negative
     */
    Product updateStock(UUID productId, int quantity);

    /**
     * Returns the current stock level for a product without going through
     * the cache (always reads from the database).
     *
     * @param productId the product to check
     * @return current stock quantity
     * @throws ProductNotFoundException if no product exists with that id
     */
    int getStockLevel(UUID productId);
}
