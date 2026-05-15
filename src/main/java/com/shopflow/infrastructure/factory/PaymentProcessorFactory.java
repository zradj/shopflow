package com.shopflow.infrastructure.factory;

import com.shopflow.domain.model.PaymentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the correct {@link PaymentProcessor} for a given
 * {@link PaymentType} at runtime.
 *
 * <h2>Factory Method Pattern (Creational #2)</h2>
 * <p>This class is the <em>Creator</em> in the Factory Method pattern. It
 * does not instantiate processors itself — Spring does that. Instead, it
 * receives every {@link PaymentProcessor} implementation as a list (Spring
 * collects all beans of a given type automatically) and indexes them by the
 * payment type they declare via {@link PaymentProcessor#supports()}.
 *
 * <h2>Self-registering registry</h2>
 * <p>Because the factory is built from Spring's bean list, adding a new
 * processor (e.g. {@code CryptoProcessor}) requires only:
 * <ol>
 *   <li>Creating the new class, annotated {@code @Component}.</li>
 *   <li>Adding a corresponding {@link PaymentType} enum value.</li>
 * </ol>
 * No modification to this factory is needed — a clean application of OCP.
 *
 * <h2>Contrast with {@code DiscountStrategyFactory}</h2>
 * <p>The discount factory uses an explicit {@code Map.of(...)} because the
 * tier-to-strategy mapping is a business rule worth making visible.
 * This factory uses auto-discovery because payment processors are pure
 * infrastructure — extensibility matters more than explicitness here.
 */
@Slf4j
@Component
public class PaymentProcessorFactory {

    /** Registry built once at startup from all {@code PaymentProcessor} beans. */
    private final Map<PaymentType, PaymentProcessor> registry;

    /**
     * Spring injects all {@link PaymentProcessor} beans as a list.
     * The factory indexes them by their declared {@link PaymentType}.
     *
     * @param processors all {@code PaymentProcessor} beans found in the context
     * @throws IllegalStateException if two processors declare the same type
     */
    public PaymentProcessorFactory(List<PaymentProcessor> processors) {
        this.registry = processors.stream()
                .collect(Collectors.toMap(
                        PaymentProcessor::supports,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                "Duplicate PaymentProcessor for type '%s': %s and %s"
                                    .formatted(a.supports(), a.getClass().getSimpleName(),
                                               b.getClass().getSimpleName()));
                        }
                ));

        log.info("PaymentProcessorFactory initialised with {} processor(s): {}",
                registry.size(),
                registry.keySet());
    }

    /**
     * Returns the {@link PaymentProcessor} registered for the given type.
     *
     * @param type the payment method chosen by the customer
     * @return the matching processor; never {@code null}
     * @throws UnsupportedPaymentTypeException if no processor is registered
     *                                         for the given type
     */
    public PaymentProcessor getProcessor(PaymentType type) {
        PaymentProcessor processor = registry.get(type);
        if (processor == null) {
            throw new UnsupportedPaymentTypeException(type);
        }
        log.debug("Resolved processor '{}' for payment type {}",
                processor.getClass().getSimpleName(), type);
        return processor;
    }

    // ── Inner exception — scoped to the factory's own concern ────────

    /**
     * Thrown when {@link #getProcessor} is called with a
     * {@link PaymentType} for which no processor is registered.
     */
    public static class UnsupportedPaymentTypeException extends RuntimeException {
        public UnsupportedPaymentTypeException(PaymentType type) {
            super("No PaymentProcessor registered for type: " + type);
        }
    }
}
