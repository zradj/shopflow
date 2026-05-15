package com.shopflow.application.service;

import java.util.UUID;

/**
 * Thrown by {@link InventoryService} when a product does not have enough
 * stock to fulfil the requested quantity.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId, int requested, int available) {
        super("Insufficient stock for product %s: requested %d, available %d"
                .formatted(productId, requested, available));
    }
}
