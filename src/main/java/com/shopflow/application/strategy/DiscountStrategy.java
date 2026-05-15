package com.shopflow.application.strategy;

import java.math.BigDecimal;

/**
 * Strategy contract for computing the discount on an order.
 *
 * <h2>Strategy Pattern (Behavioral #1)</h2>
 * <p>Each concrete implementation encapsulates one pricing rule. The
 * {@code OrderFacade} selects the appropriate strategy at runtime based on
 * the customer's {@link com.shopflow.domain.model.CustomerTier}, then calls
 * {@link #calculate} — with no {@code if/else} or {@code switch} in the
 * facade itself.
 *
 * <h2>Open/Closed Principle</h2>
 * <p>Adding a new discount rule (e.g. a flash-sale strategy) means writing a
 * new class that implements this interface and registering it in
 * {@link DiscountStrategyFactory}. No existing class is modified.
 *
 * <h2>Why {@code BigDecimal subtotal} rather than {@code Order}</h2>
 * <p>The facade must know the discount amount <em>before</em> calling
 * {@code Order.Builder.build()} — the discount is an input to the builder,
 * not a property of the finished order. Taking the subtotal directly removes
 * the circular dependency while keeping each strategy's logic minimal and
 * easily unit-testable with a single numeric argument.
 */
public interface DiscountStrategy {

    /**
     * Calculates the absolute discount amount for the given subtotal.
     *
     * @param subtotal the pre-discount order total, as accumulated by
     *                 {@code Order.Builder} after all items are added
     * @return a non-negative discount amount, never exceeding the subtotal
     */
    BigDecimal calculate(BigDecimal subtotal);

    /**
     * A human-readable label for this strategy, used in logs and the
     * order audit trail.
     */
    String describe();
}
