package com.shopflow.infrastructure.factory;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.PaymentType;

/**
 * Contract that every payment processor must fulfil.
 *
 * <h2>Factory Method Pattern — Product Interface</h2>
 * <p>In the Factory Method pattern, this interface is the <em>Product</em>.
 * {@link PaymentProcessorFactory} is the <em>Creator</em>, and the concrete
 * classes ({@link CreditCardProcessor}, {@link WalletProcessor}) are the
 * <em>Concrete Products</em>. The {@code OrderFacade} depends only on this
 * interface — it never references a concrete processor directly.
 *
 * <p>All external gateway communication is delegated to
 * {@link com.shopflow.infrastructure.gateway.PaymentGatewayClient}, which
 * carries the Resilience4j Circuit Breaker annotation. This keeps the
 * processor classes free of resilience infrastructure (SRP).
 */
public interface PaymentProcessor {

    /**
     * Processes the payment for the given order.
     *
     * @param order the order to charge; uses {@link Order#getTotalAmount()}
     *              as the amount to debit
     * @return a {@link PaymentResult} describing the outcome —
     *         never {@code null}, never throws for business failures
     */
    PaymentResult process(Order order);

    /**
     * The payment type this processor handles. Used by
     * {@link PaymentProcessorFactory} to populate its registry and
     * for logging/audit purposes.
     */
    PaymentType supports();
}
