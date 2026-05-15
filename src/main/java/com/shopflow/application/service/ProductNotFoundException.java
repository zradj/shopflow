package com.shopflow.application.service;

import java.util.UUID;

/**
 * Thrown when a {@link com.shopflow.domain.model.Product} cannot be found
 * by the requested identifier.
 *
 * <p>Kept in the {@code application.service} package so that both
 * {@code ProductServiceImpl} and {@code CachingProductService} can reference
 * it without a cross-layer import.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id) {
        super("Product not found with id: " + id);
    }

    public ProductNotFoundException(String message) {
        super(message);
    }
}
