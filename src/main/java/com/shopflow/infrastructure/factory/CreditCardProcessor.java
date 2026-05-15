package com.shopflow.infrastructure.factory;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.PaymentType;
import com.shopflow.infrastructure.gateway.PaymentGatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles payment processing for credit card orders.
 *
 * <h2>Factory Method Pattern — Concrete Product</h2>
 * <p>This class is one of two concrete products created by
 * {@link PaymentProcessorFactory}. It implements the {@link PaymentProcessor}
 * interface and is selected when the order's payment type is
 * {@link PaymentType#CREDIT_CARD}.
 *
 * <h2>Single Responsibility</h2>
 * <p>This class is responsible for one thing only: preparing the credit-card
 * specific context and delegating the actual charge to
 * {@link PaymentGatewayClient}. Resilience logic (circuit breaking, retries)
 * lives exclusively in the gateway client implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditCardProcessor implements PaymentProcessor {

    private final PaymentGatewayClient gatewayClient;

    /**
     * Submits a credit card charge through the gateway client.
     *
     * <p>Any gateway-level failure (timeout, circuit open) is handled inside
     * {@code MockExternalPaymentGatewayClient} and surfaced as a
     * {@link PaymentResult#failure} result — this method never throws for
     * business failures.
     */
    @Override
    public PaymentResult process(Order order) {
        log.info("Processing CREDIT_CARD payment for order {} — amount={}",
                order.getId(), order.getTotalAmount());

        PaymentResult result = gatewayClient.charge(
                "CREDIT_CARD",
                order.getTotalAmount(),
                order.getId().toString()
        );

        if (result.isSuccessful()) {
            log.info("CREDIT_CARD payment succeeded — order={}, ref={}",
                    order.getId(), result.getTransactionReference());
        } else {
            log.warn("CREDIT_CARD payment failed — order={}, reason={}",
                    order.getId(), result.getFailureReason());
        }

        return result;
    }

    @Override
    public PaymentType supports() {
        return PaymentType.CREDIT_CARD;
    }
}
