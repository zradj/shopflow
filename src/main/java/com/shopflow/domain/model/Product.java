package com.shopflow.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a product available for purchase.
 *
 * <p>{@link #price} uses {@link BigDecimal} to guarantee exact decimal
 * arithmetic — never {@code double} for monetary values.
 *
 * <p>This entity is the primary target of the Redis Cache-Aside layer
 * implemented in {@code CachingProductService}. Stock mutations via
 * {@code InventoryService} explicitly evict the cache entry.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Unit price. Must be non-negative.
     * Stored with scale 2 to match standard currency precision.
     */
    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * Current stock level. Managed exclusively by {@code InventoryService}
     * to maintain a single point of truth (SRP).
     */
    @Min(0)
    @Column(nullable = false)
    private int stockQuantity;

    @NotBlank
    @Column(nullable = false)
    private String category;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Domain behaviour ─────────────────────────────────────────────

    /**
     * Returns {@code true} if at least {@code quantity} units are in stock.
     */
    public boolean hasStock(int quantity) {
        return this.stockQuantity >= quantity;
    }

    /**
     * Deducts {@code quantity} units. Call only after {@link #hasStock}.
     *
     * @throws IllegalStateException if the deduction would make stock negative
     */
    public void deductStock(int quantity) {
        if (!hasStock(quantity)) {
            throw new IllegalStateException(
                "Insufficient stock for product '%s': requested %d, available %d"
                    .formatted(name, quantity, stockQuantity)
            );
        }
        this.stockQuantity -= quantity;
    }

    // ── Factory method for convenience in tests & seeders ────────────

    public static Product of(String name, String description,
                             BigDecimal price, int stockQuantity,
                             String category) {
        Product p = new Product();
        p.name          = name;
        p.description   = description;
        p.price         = price;
        p.stockQuantity = stockQuantity;
        p.category      = category;
        return p;
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price
               + ", stock=" + stockQuantity + "}";
    }
}
