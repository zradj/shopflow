package com.shopflow.infrastructure.repository;

import com.shopflow.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Product}.
 *
 * <p>Intentionally kept in the {@code infrastructure} package — the
 * application layer talks to {@code ProductService}, never to this
 * repository directly (Dependency Inversion Principle).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /** Returns all products belonging to the given category. */
    List<Product> findByCategory(String category);

    /** Returns only products that currently have stock available. */
    List<Product> findByStockQuantityGreaterThan(int minStock);
}
