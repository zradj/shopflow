package com.shopflow.application.service;

import com.shopflow.domain.model.Product;
import com.shopflow.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Database-backed implementation of {@link ProductService}.
 *
 * <h2>Single Responsibility</h2>
 * <p>This class has exactly one job: translate {@link ProductService} calls
 * into JPA repository queries. It has no knowledge of caching, HTTP, or
 * business rules such as discounting.
 *
 * <h2>Decorator target</h2>
 * <p>{@code CachingProductService} wraps this bean. All cache misses
 * eventually delegate here. This class is annotated {@code @Service} so
 * Spring registers it, but callers that need caching should inject the
 * {@code ProductService} interface (which resolves to
 * {@code CachingProductService} due to the {@code @Primary} annotation on
 * that bean).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // all reads are non-mutating by default
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Queries the database directly. In production traffic this method is
     * called only on a cache miss inside {@code CachingProductService}.
     */
    @Override
    public Product findById(UUID id) {
        log.debug("DB query — findById({})", id);
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Product> findAll() {
        log.debug("DB query — findAll()");
        return productRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Product> findByCategory(String category) {
        log.debug("DB query — findByCategory({})", category);
        return productRepository.findByCategory(category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Product> findInStock() {
        log.debug("DB query — findInStock()");
        return productRepository.findByStockQuantityGreaterThan(0);
    }
}
