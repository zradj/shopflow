package com.shopflow.application.strategy;

import com.shopflow.application.strategy.DiscountStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Applies a percentage discount to the order subtotal.
 * Used for {@link com.shopflow.domain.model.CustomerTier#PREMIUM} customers.
 *
 * <p>The rate is injected from {@code application.properties} so it can be
 * adjusted without touching code — a practical demonstration of OCP at the
 * configuration level.
 *
 * <p>{@link RoundingMode#HALF_UP} is used for monetary rounding, which
 * matches standard retail and accounting convention.
 */
@Component
public class PercentageDiscountStrategy implements DiscountStrategy {

    private final BigDecimal rate;

    /**
     * @param ratePercent the discount percentage (e.g. {@code 10.0} for 10 %).
     *                    Defaults to 10 % if not configured.
     */
    public PercentageDiscountStrategy(
            @Value("${shopflow.discount.premium-rate-percent:10.0}") double ratePercent) {
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
        return "Percentage discount @ %s%% (PREMIUM tier)"
                .formatted(rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString());
    }
}
