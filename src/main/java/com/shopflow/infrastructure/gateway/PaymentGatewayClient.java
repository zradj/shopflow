package com.shopflow.infrastructure.gateway;

import com.shopflow.infrastructure.factory.PaymentResult;

import java.math.BigDecimal;

/**
 * Abstraction over the external payment gateway HTTP call.
 *
 * <p>Defining this as an interface now means the processors ({@code
 * CreditCardProcessor}, {@code WalletProcessor}) can be written and tested
 * immediately, while the concrete implementation — which will carry the
 * Resilience4j {@code @CircuitBreaker} annotation — is added in step 9
 * without any change to the processors themselves (OCP + DIP).
 *
 * <p>The circuit breaker and its fallback logic belong on the implementation,
 * not on this interface, keeping the contract clean and infrastructure-agnostic.
 */
public interface PaymentGatewayClient {

    /**
     * Submits a charge request to the external payment gateway.
     *
     * @param paymentMethod a descriptor string passed to the gateway
     *                      (e.g. {@code "CREDIT_CARD"}, {@code "WALLET"})
     * @param amount        the exact amount to charge
     * @param orderId       a correlation ID for the gateway's audit log
     * @return a {@link PaymentResult} describing the gateway's response
     */
    PaymentResult charge(String paymentMethod, BigDecimal amount, String orderId);
}
