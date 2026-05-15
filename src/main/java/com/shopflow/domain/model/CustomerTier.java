package com.shopflow.domain.model;

/**
 * Represents a customer's loyalty tier.
 * Used by the DiscountStrategy to select the appropriate pricing rule
 * without any conditional logic in the order flow (OCP).
 */
public enum CustomerTier {

    /** No loyalty benefits applied. */
    STANDARD,

    /** 10% discount on all orders. */
    PREMIUM,

    /** 20% discount on all orders. */
    VIP
}
