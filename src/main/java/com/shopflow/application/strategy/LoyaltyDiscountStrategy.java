package com.shopflow.application.strategy;

import com.shopflow.application.strategy.DiscountStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Applies a higher percentage discount for top-tier loyal customers.
 * Used for {@link com.shopflow.domain.model.CustomerTier#VIP} customers.
 *
 * <p>Structurally identical to {@link PercentageDiscountStrategy} but with
 * its own injected rate and its own Spring bean identity. This deliberate
 * separation means the two rates can be configured and changed independently,
 * and each strategy remains a single cohesive unit (SRP).
 */
@Component
public class LoyaltyDiscountStrategy implements DiscountStrategy {

    private final BigDecimal rate;

    /**
     * @param ratePercent the VIP discount percentage (e.g. {@code 20.0} for 20 %).
     *                    Defaults to 20 % if not configured.
     */
    public LoyaltyDiscountStrategy(
            @Value("${shopflow.discount.vip-rate-percent:20.0}") double ratePercent) {
        this.rate = BigDecimal.valueOf(ratePercent)
                              .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /**
     * Returns {@code subtotal × rate}, rounded to 2 decimal places.
     */
    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return subtotal
                    .multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String describe() {
        return "Loyalty discount @ %s%% (VIP tier)"
                .formatted(rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
    }
}
