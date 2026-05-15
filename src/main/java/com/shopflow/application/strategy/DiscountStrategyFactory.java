package com.shopflow.application.strategy;

import com.shopflow.domain.model.CustomerTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the correct {@link DiscountStrategy} for a given
 * {@link CustomerTier} at runtime.
 *
 * <h2>Factory Method Pattern (Creational #1 — second application)</h2>
 * <p>This factory encapsulates the mapping logic between a customer's tier
 * and the discount rule that applies to them. The {@code OrderFacade} calls
 * {@link #getStrategy} and receives a strategy — it never instantiates or
 * selects one itself. This keeps the facade free of tier-based conditionals
 * and satisfies OCP: a new tier just needs a new strategy class and a new
 * entry in the map.
 *
 * <h2>Why a Map instead of a switch/if-else?</h2>
 * <p>A {@code switch} on {@code CustomerTier} would require modification
 * every time a tier is added, violating OCP. The {@link Map} is built once
 * at construction and never touched again. Adding a new tier is a matter of
 * adding a new strategy bean and a new map entry — not editing a conditional.
 *
 * <h2>Dependency injection</h2>
 * <p>Each strategy is a Spring {@code @Component}, so they are injected
 * directly into the constructor. No manual instantiation occurs here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscountStrategyFactory {

    private final NoDiscountStrategy         noDiscountStrategy;
    private final PercentageDiscountStrategy percentageDiscountStrategy;
    private final LoyaltyDiscountStrategy    loyaltyDiscountStrategy;

    /**
     * Returns the {@link DiscountStrategy} appropriate for the given tier.
     *
     * @param tier the customer's loyalty tier
     * @return the matching strategy; never {@code null}
     */
    public DiscountStrategy getStrategy(CustomerTier tier) {
        Map<CustomerTier, DiscountStrategy> strategies = Map.of(
                CustomerTier.STANDARD, noDiscountStrategy,
                CustomerTier.PREMIUM,  percentageDiscountStrategy,
                CustomerTier.VIP,      loyaltyDiscountStrategy
        );

        DiscountStrategy strategy = strategies.getOrDefault(tier, noDiscountStrategy);
        log.debug("Selected discount strategy '{}' for tier {}", strategy.describe(), tier);
        return strategy;
    }
}
