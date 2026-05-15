package com.shopflow.application.service;

import com.shopflow.domain.model.Product;

import java.util.List;
import java.util.UUID;

/**
 * Application-level contract for product retrieval.
 *
 * <h2>Why an interface matters here (OCP + Decorator)</h2>
 * <p>Controllers and the {@code OrderFacade} depend only on this interface,
 * never on a concrete class. That makes it trivial to swap in
 * {@code CachingProductService} (which wraps {@code ProductServiceImpl})
 * without any change to the callers — a textbook application of the
 * Open/Closed Principle and the Decorator structural pattern.
 *
 * <p>Write operations (stock deduction, price updates) are the
 * responsibility of {@code InventoryService}, not this interface, honouring
 * the Single Responsibility Principle.
 */
public interface ProductService {

    /**
     * Retrieves a single product by its unique identifier.
     *
     * @param id the product UUID
     * @return the matching product
     * @throws ProductNotFoundException if no product exists with that id
     */
    Product findById(UUID id);

    /**
     * Returns every product in the catalogue.
     */
    List<Product> findAll();

    /**
     * Returns all products belonging to the specified category.
     *
     * @param category case-sensitive category name
     */
    List<Product> findByCategory(String category);

    /**
     * Returns only products that currently have at least one unit in stock.
     */
    List<Product> findInStock();
}
