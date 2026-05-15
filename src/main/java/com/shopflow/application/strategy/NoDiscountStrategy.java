package com.shopflow.application.strategy;

import com.shopflow.application.strategy.DiscountStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * A no-op discount strategy applied to
 * {@link com.shopflow.domain.model.CustomerTier#STANDARD} customers.
 *
 * <p>Returning {@link BigDecimal#ZERO} rather than using {@code null}
 * eliminates null-checks everywhere the result is used and keeps arithmetic
 * consistent across all strategy implementations.
 */
@Component
public class NoDiscountStrategy implements DiscountStrategy {

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public String describe() {
        return "No discount (STANDARD tier)";
    }
}
