package com.shopflow.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single line item within an {@link Order}.
 *
 * <p>Unit price is snapshot at order-creation time so that future product
 * price changes never alter historical order totals.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Min(1)
    @Column(nullable = false, updatable = false)
    private int quantity;

    /**
     * Price per unit at the moment the order was placed.
     * Immutable after creation.
     */
    @NotNull
    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Pre-computed line total: {@code unitPrice × quantity}.
     * Stored so the DB can be queried for line totals without
     * in-memory arithmetic.
     */
    @NotNull
    @Column(nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    // ── Package-private constructor — only Order.Builder should call this ──

    OrderItem(Product product, int quantity) {
        this.product   = product;
        this.quantity  = quantity;
        this.unitPrice = product.getPrice();
        this.subtotal  = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return "OrderItem{product='%s', qty=%d, unitPrice=%s, subtotal=%s}"
            .formatted(product.getName(), quantity, unitPrice, subtotal);
    }
}
