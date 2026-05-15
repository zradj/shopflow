package com.shopflow.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root for the order lifecycle.
 *
 * <h2>Builder Pattern (Creational #1)</h2>
 * <p>Orders have many fields, several of which are optional at construction
 * time (e.g. discount, notes). A telescoping constructor would be both
 * unreadable and fragile. The inner {@link Builder} provides a fluent,
 * validated, step-by-step construction API. The {@code Order} constructor is
 * private — the only legal way to create an instance is through the Builder.
 *
 * <h2>JPA note</h2>
 * <p>JPA requires a no-arg constructor; it is declared {@code protected} so
 * the framework can use it via reflection while keeping it inaccessible to
 * application code.
 *
 * <pre>{@code
 * Order order = new Order.Builder(customer, PaymentType.CREDIT_CARD, address)
 *     .addItem(laptop, 1)
 *     .addItem(mouse, 2)
 *     .discountAmount(new BigDecimal("50.00"))
 *     .notes("Leave at the door")
 *     .build();
 * }</pre>
 */
@Entity
@Table(name = "orders")
@Getter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Sum of all line-item subtotals before any discount.
     */
    @NotNull
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    /**
     * Discount amount in absolute currency units.
     * Calculated by the active {@code DiscountStrategy} in
     * {@code OrderFacade} and passed to the Builder.
     */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    /**
     * Final amount charged: {@code subtotal − discountAmount}.
     */
    @NotNull
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentType paymentType;

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant placedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected Order() {}

    /** Private — created only by {@link Builder#build()}. */
    private Order(Builder builder) {
        this.customer        = builder.customer;
        this.items           = builder.items;
        this.subtotal        = builder.subtotal;
        this.discountAmount  = builder.discountAmount;
        this.totalAmount     = builder.subtotal.subtract(builder.discountAmount);
        this.status          = OrderStatus.PENDING;
        this.paymentType     = builder.paymentType;
        this.shippingAddress = builder.shippingAddress;
        this.notes           = builder.notes;
        this.placedAt        = Instant.now();
        this.updatedAt       = this.placedAt;
    }

    // ── Domain behaviour ─────────────────────────────────────────────

    /**
     * Transitions the order to a new status.
     * All status changes go through this single method (SRP) so that
     * audit logging and validation can be added here without touching callers.
     */
    public void transitionTo(OrderStatus newStatus) {
        this.status    = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns an unmodifiable view of the order's line items.
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Builder  (GoF Creational Pattern #1)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Fluent builder for {@link Order}.
     *
     * <p>Required fields must be supplied to the constructor.
     * Optional fields ({@link #discountAmount}, {@link #notes}) have
     * sensible defaults and can be omitted.
     */
    public static final class Builder {

        // Required
        private final Customer        customer;
        private final PaymentType     paymentType;
        private final ShippingAddress shippingAddress;

        // Accumulates line items as the caller adds them
        private final List<OrderItem> items = new ArrayList<>();

        // Computed once build() is called
        private BigDecimal subtotal = BigDecimal.ZERO;

        // Optional — defaults applied here
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private String     notes;

        /**
         * @param customer        the customer placing the order (required)
         * @param paymentType     selected payment method (required)
         * @param shippingAddress delivery address (required)
         */
        public Builder(Customer customer,
                       PaymentType paymentType,
                       ShippingAddress shippingAddress) {

            if (customer        == null) throw new IllegalArgumentException("Customer is required");
            if (paymentType     == null) throw new IllegalArgumentException("PaymentType is required");
            if (shippingAddress == null) throw new IllegalArgumentException("ShippingAddress is required");

            this.customer        = customer;
            this.paymentType     = paymentType;
            this.shippingAddress = shippingAddress;
        }

        /**
         * Adds a line item to the order and accumulates the subtotal.
         *
         * @param product  the product to order
         * @param quantity units requested (must be ≥ 1)
         * @return this builder (fluent)
         */
        public Builder addItem(Product product, int quantity) {
            if (product  == null) throw new IllegalArgumentException("Product must not be null");
            if (quantity < 1)    throw new IllegalArgumentException("Quantity must be at least 1");

            OrderItem item = new OrderItem(product, quantity);
            items.add(item);
            subtotal = subtotal.add(item.getSubtotal());
            return this;
        }

        /**
         * Returns the subtotal accumulated so far from all added items.
         *
         * <p>Called by {@code OrderFacade} after all items have been added
         * so the active {@link com.shopflow.application.strategy.DiscountStrategy}
         * can compute the discount from the running total before
         * {@link #build()} is called.
         */
        public BigDecimal getCurrentSubtotal() {
            return subtotal;
        }

        /**
         * Applies an absolute discount to the order total.
         * The {@code OrderFacade} calculates this via the active
         * {@code DiscountStrategy} before calling the builder.
         *
         * @param amount must be ≥ 0 and ≤ subtotal
         * @return this builder (fluent)
         */
        public Builder discountAmount(BigDecimal amount) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Discount must be a non-negative value");
            }
            if (amount.compareTo(subtotal) > 0) {
                throw new IllegalArgumentException(
                    "Discount (%s) cannot exceed subtotal (%s)".formatted(amount, subtotal)
                );
            }
            this.discountAmount = amount;
            return this;
        }

        /**
         * Optional delivery note visible to the warehouse.
         *
         * @return this builder (fluent)
         */
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        /**
         * Validates state and constructs the immutable {@link Order}.
         *
         * @throws IllegalStateException if no items have been added
         */
        public Order build() {
            if (items.isEmpty()) {
                throw new IllegalStateException("An order must contain at least one item");
            }
            return new Order(this);
        }
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", customer=" + customer.getEmail()
               + ", total=" + totalAmount + ", status=" + status + "}";
    }
}
