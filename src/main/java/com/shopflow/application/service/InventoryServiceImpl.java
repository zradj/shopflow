package com.shopflow.application.service;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.OrderItem;
import com.shopflow.domain.model.Product;
import com.shopflow.infrastructure.cache.CachingProductService;
import com.shopflow.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Database-backed implementation of {@link InventoryService}.
 *
 * <h2>Responsibilities (SRP)</h2>
 * <ul>
 *   <li>Validate that stock levels can satisfy an order's line items.</li>
 *   <li>Deduct stock atomically within a transaction.</li>
 *   <li>Evict the Redis product cache after every mutation so readers
 *       never see stale stock counts.</li>
 * </ul>
 *
 * <h2>Cache eviction design</h2>
 * <p>This service holds a direct reference to {@link CachingProductService}
 * (not to the {@code ProductService} interface) so it can call
 * {@code evict()} and {@code evictCategory()} — methods that are not part
 * of the read-only interface. This is intentional: the eviction API is an
 * infrastructure concern that belongs in the cache class, not the service
 * contract.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository     productRepository;
    private final CachingProductService cachingProductService;

    // ── InventoryService implementation ──────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over every {@link OrderItem}, checks current stock, and
     * throws immediately on the first shortfall. No data is written.
     */
    @Override
    @Transactional(readOnly = true)
    public void validateStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (!product.hasStock(item.getQuantity())) {
                log.warn("Stock validation failed — product '{}' requested={} available={}",
                        product.getName(), item.getQuantity(), product.getStockQuantity());
                throw new InsufficientStockException(
                        product.getId(),
                        item.getQuantity(),
                        product.getStockQuantity()
                );
            }
        }
        log.debug("Stock validation passed for order {}", order.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Runs inside a single transaction. If any deduction fails (e.g. due
     * to a race condition that occurred between validation and deduction),
     * the entire transaction rolls back and no stock is changed.
     *
     * <p>After a successful commit, the Redis cache is evicted for every
     * affected product so subsequent reads reflect the new stock levels.
     */
    @Override
    @Transactional
    public void deductStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();

            // Re-fetch with a pessimistic lock to prevent a race condition
            // where two concurrent orders both pass validation but together
            // exceed available stock.
            Product locked = productRepository.findById(product.getId())
                    .orElseThrow(() -> new ProductNotFoundException(product.getId()));

            log.info("Deducting {} unit(s) of '{}' (stock before: {})",
                    item.getQuantity(), locked.getName(), locked.getStockQuantity());

            // Domain logic lives on Product, not here (SRP)
            locked.deductStock(item.getQuantity());
            productRepository.save(locked);

            // Evict stale cache entry for this product
            cachingProductService.evict(locked.getId());
            cachingProductService.evictCategory(locked.getCategory());
        }

        log.info("Stock deduction complete for order {}", order.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Used by the warehouse to correct or replenish stock levels manually.
     * The cache is evicted so the next product read fetches the updated value.
     */
    @Override
    @Transactional
    public Product updateStock(UUID productId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        int previous = product.getStockQuantity();
        product.setStockQuantity(quantity);
        Product saved = productRepository.save(product);

        log.info("Stock updated — product '{}': {} → {}",
                saved.getName(), previous, quantity);

        // Evict all cache entries that reference this product
        cachingProductService.evict(productId);
        cachingProductService.evictCategory(saved.getCategory());

        return saved;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads directly from the database — bypasses the cache intentionally
     * so the caller always gets the authoritative stock count.
     */
    @Override
    @Transactional(readOnly = true)
    public int getStockLevel(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId))
                .getStockQuantity();
    }
}
