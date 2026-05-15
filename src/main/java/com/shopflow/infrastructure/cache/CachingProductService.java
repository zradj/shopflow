package com.shopflow.infrastructure.cache;

import com.shopflow.application.service.ProductNotFoundException;
import com.shopflow.application.service.ProductService;
import com.shopflow.domain.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Caching decorator for {@link ProductService}.
 *
 * <h2>Decorator Pattern (Structural #2)</h2>
 * <p>This class wraps the real {@code ProductServiceImpl} via the shared
 * {@code ProductService} interface. Every method either serves data from
 * Redis or falls back to the delegate and writes the result into the cache.
 * The {@code OrderFacade} and controllers know only the {@code ProductService}
 * interface — they are completely unaware that a caching layer exists.
 *
 * <h2>Cache-Aside Pattern</h2>
 * <pre>
 *   READ:
 *     1. Check Redis for the key.
 *     2. HIT  → return cached value.
 *     3. MISS → call delegate (DB), write result into Redis with TTL, return.
 *
 *   WRITE (stock mutation via InventoryService):
 *     → Calls {@link #evict(UUID)} to keep the cache consistent.
 *        InventoryService is responsible for calling evict(); this class
 *        only exposes the mechanism.
 * </pre>
 *
 * <h2>Why @Primary?</h2>
 * <p>Both {@code ProductServiceImpl} and this class implement
 * {@code ProductService}. Marking this bean {@code @Primary} means Spring
 * injects the caching decorator wherever a {@code ProductService} is
 * autowired, without any change to the injection sites.
 */
@Slf4j
@Service
@Primary
public class CachingProductService implements ProductService {

    // ── Cache key constants ───────────────────────────────────────────

    private static final String PREFIX_SINGLE  = "product::";
    private static final String KEY_ALL        = "product::all";
    private static final String PREFIX_CATEGORY = "product::category::";
    private static final String KEY_IN_STOCK   = "product::in_stock";

    // ── Dependencies ─────────────────────────────────────────────────

    private final ProductService              delegate;
    private final RedisTemplate<String, Object> redis;

    /** How long cached entries live. Injected from application properties. */
    private final Duration cacheTtl;

    // ── Constructor ───────────────────────────────────────────────────

    /**
     * @param delegate   the real {@code ProductServiceImpl} (injected by name
     *                   to avoid ambiguity with this bean itself)
     * @param redis      the configured {@link RedisTemplate}
     * @param cacheTtlMs TTL in milliseconds from {@code shopflow.cache.product-ttl-ms}
     */
    public CachingProductService(
            @Qualifier("productServiceImpl") ProductService delegate,
            RedisTemplate<String, Object> redis,
            @Value("${shopflow.cache.product-ttl-ms:300000}") long cacheTtlMs) {

        this.delegate = delegate;
        this.redis    = redis;
        this.cacheTtl = Duration.ofMillis(cacheTtlMs);
    }

    // ── ProductService implementation ─────────────────────────────────

    /**
     * Returns a single product, using the cache key {@code "product::{id}"}.
     */
    @Override
    public Product findById(UUID id) {
        String key = PREFIX_SINGLE + id;

        Product cached = (Product) redis.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache HIT  — findById({})", id);
            return cached;
        }

        log.debug("Cache MISS — findById({}) → querying DB", id);
        Product product = delegate.findById(id);   // throws ProductNotFoundException on miss
        redis.opsForValue().set(key, product, cacheTtl);
        return product;
    }

    /**
     * Returns all products, using the cache key {@code "product::all"}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Product> findAll() {
        List<Product> cached = (List<Product>) redis.opsForValue().get(KEY_ALL);
        if (cached != null) {
            log.debug("Cache HIT  — findAll()");
            return cached;
        }

        log.debug("Cache MISS — findAll() → querying DB");
        List<Product> products = delegate.findAll();
        redis.opsForValue().set(KEY_ALL, products, cacheTtl);
        return products;
    }

    /**
     * Returns products in a category, using key
     * {@code "product::category::{category}"}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Product> findByCategory(String category) {
        String key = PREFIX_CATEGORY + category;

        List<Product> cached = (List<Product>) redis.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache HIT  — findByCategory({})", category);
            return cached;
        }

        log.debug("Cache MISS — findByCategory({}) → querying DB", category);
        List<Product> products = delegate.findByCategory(category);
        redis.opsForValue().set(key, products, cacheTtl);
        return products;
    }

    /**
     * Returns in-stock products, using key {@code "product::in_stock"}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Product> findInStock() {
        List<Product> cached = (List<Product>) redis.opsForValue().get(KEY_IN_STOCK);
        if (cached != null) {
            log.debug("Cache HIT  — findInStock()");
            return cached;
        }

        log.debug("Cache MISS — findInStock() → querying DB");
        List<Product> products = delegate.findInStock();
        redis.opsForValue().set(KEY_IN_STOCK, products, cacheTtl);
        return products;
    }

    // ── Cache management ─────────────────────────────────────────────

    /**
     * Evicts all cache entries that could contain stale data for the given
     * product after a stock or price update.
     *
     * <p>Called by {@code InventoryService} after any mutation so that the
     * next read re-fetches fresh data from the database. This is the "write"
     * side of the Cache-Aside pattern.
     *
     * @param productId the ID of the product whose cached data is now stale
     */
    public void evict(UUID productId) {
        log.info("Cache EVICT — product {}", productId);

        // Evict the individual product entry
        redis.delete(PREFIX_SINGLE + productId);

        // Also evict list-level caches — they contain snapshots of this product
        redis.delete(KEY_ALL);
        redis.delete(KEY_IN_STOCK);

        // Note: category-level caches are NOT evicted here because we would
        // need the product's category to build the key. InventoryService
        // can call evictCategory() separately if it has that information.
    }

    /**
     * Evicts the category-level cache entry for the given category.
     * Call this from {@code InventoryService} when the product's category
     * is known at the time of the mutation.
     *
     * @param category the category whose cached list is now stale
     */
    public void evictCategory(String category) {
        log.info("Cache EVICT — category '{}'", category);
        redis.delete(PREFIX_CATEGORY + category);
    }

    /**
     * Flushes every product-related cache entry.
     * Use sparingly (e.g. after a bulk import); individual evictions are
     * preferred for normal operations to avoid a "thundering herd" on Redis.
     */
    public void evictAll() {
        log.warn("Cache EVICT ALL — flushing all product cache entries");
        redis.delete(KEY_ALL);
        redis.delete(KEY_IN_STOCK);
        // Pattern-based deletion for product:: and product::category:: keys
        var keys = redis.keys(PREFIX_SINGLE + "*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
        var catKeys = redis.keys(PREFIX_CATEGORY + "*");
        if (catKeys != null && !catKeys.isEmpty()) redis.delete(catKeys);
    }
}
