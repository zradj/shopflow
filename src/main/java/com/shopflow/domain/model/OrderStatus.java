package com.shopflow.domain.model;

/**
 * Lifecycle states of an {@link Order}.
 *
 * <pre>
 *   PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 *       ↘ CANCELLED (from PENDING or CONFIRMED only)
 *       ↘ PAYMENT_FAILED (from PENDING, via circuit-breaker fallback)
 * </pre>
 */
public enum OrderStatus {

    /** Payment initiated; awaiting gateway confirmation. */
    PENDING,

    /** Payment confirmed by the external gateway. */
    CONFIRMED,

    /** Warehouse is packing the order. */
    PROCESSING,

    /** Order dispatched to the carrier. */
    SHIPPED,

    /** Order received by the customer. */
    DELIVERED,

    /** Order was cancelled before shipment. */
    CANCELLED,

    /**
     * External payment gateway was unavailable.
     * Triggered by the Circuit Breaker fallback in
     * {@code MockExternalPaymentGatewayClient}.
     */
    PAYMENT_FAILED
}
