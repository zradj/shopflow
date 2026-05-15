package com.shopflow.infrastructure.factory;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.PaymentType;
import com.shopflow.infrastructure.gateway.PaymentGatewayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles payment processing for digital wallet orders.
 *
 * <h2>Factory Method Pattern — Concrete Product</h2>
 * <p>This class is the second concrete product created by
 * {@link PaymentProcessorFactory}. It handles orders whose payment type is
 * {@link PaymentType#WALLET}.
 *
 * <p>Wallet payments are treated as near-instant by the gateway (lower
 * latency, higher success rate than card payments in practice), but the
 * circuit breaker applies equally — the gateway client is the same
 * infrastructure boundary regardless of payment method.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletProcessor implements PaymentProcessor {

    private final PaymentGatewayClient gatewayClient;

    /**
     * Submits a wallet debit through the gateway client.
     *
     * <p>Wallet-specific validation (e.g. checking wallet balance) would be
     * added here in a real system, keeping that logic inside this processor
     * rather than in the facade.
     */
    @Override
    public PaymentResult process(Order order) {
        log.info("Processing WALLET payment for order {} — amount={}",
                order.getId(), order.getTotalAmount());

        PaymentResult result = gatewayClient.charge(
                "WALLET",
                order.getTotalAmount(),
                order.getId().toString()
        );

        if (result.isSuccessful()) {
            log.info("WALLET payment succeeded — order={}, ref={}",
                    order.getId(), result.getTransactionReference());
        } else {
            log.warn("WALLET payment failed — order={}, reason={}",
                    order.getId(), result.getFailureReason());
        }

        return result;
    }

    @Override
    public PaymentType supports() {
        return PaymentType.WALLET;
    }
}
