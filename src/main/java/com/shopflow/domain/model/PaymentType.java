package com.shopflow.domain.model;

/**
 * Supported payment methods.
 * The {@code PaymentProcessorFactory} uses this enum to select the
 * correct {@code PaymentProcessor} implementation (Factory Method pattern).
 */
public enum PaymentType {
    CREDIT_CARD,
    WALLET
}
