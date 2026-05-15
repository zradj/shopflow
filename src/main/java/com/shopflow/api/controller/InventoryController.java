package com.shopflow.api.controller;

import com.shopflow.api.dto.ProductResponse;
import com.shopflow.api.dto.UpdateStockRequest;
import com.shopflow.application.service.InventoryService;
import com.shopflow.domain.model.Product;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Exposes warehouse-level inventory management endpoints.
 *
 * <p>Intentionally separate from {@code ProductController}: reading the
 * product catalogue and mutating stock are different concerns with different
 * access patterns (public reads vs. warehouse writes). Keeping them in
 * separate controllers makes it straightforward to add different
 * authentication rules to each in the future.
 *
 * <h2>Cache eviction</h2>
 * <p>Every stock update goes through {@link InventoryService#updateStock},
 * which calls {@code CachingProductService.evict()} internally. The
 * controller does not interact with Redis directly — it remains unaware that
 * a cache layer exists.
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Sets the stock level for a product to the given absolute quantity.
     *
     * <p>This is an absolute set, not a delta — sending {@code quantity: 50}
     * means "the warehouse now has 50 units", regardless of the previous
     * value. This avoids race conditions that arise from delta-based updates
     * ({@code +10}, {@code -3}) when multiple operators submit concurrently.
     *
     * <p>The product's Redis cache entry is evicted automatically by
     * {@code InventoryService}.
     *
     * @param productId the product to update
     * @param request   the new absolute stock quantity
     * @return 200 with the updated product
     */
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateStockRequest request) {
        log.info("PATCH /api/inventory/{}/stock — newQuantity={}",
                productId, request.getQuantity());
        Product updated = inventoryService.updateStock(productId, request.getQuantity());
        return ResponseEntity.ok(ProductResponse.from(updated));
    }

    /**
     * Returns the current authoritative stock level for a product.
     *
     * <p>Always reads from the database (bypasses Redis) so the caller
     * gets the live count, not a potentially stale cached value. This is
     * useful for warehouse dashboards and pre-order validations.
     *
     * @param productId the product to check
     * @return 200 with a JSON object containing the stock count
     */
    @GetMapping("/{productId}/stock")
    public ResponseEntity<Map<String, Object>> getStockLevel(
            @PathVariable UUID productId) {
        log.debug("GET /api/inventory/{}/stock", productId);
        int stock = inventoryService.getStockLevel(productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "stockQuantity", stock
        ));
    }
}
