package com.shopflow.application.service;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable value object representing a payment instruction.
 *
 * <p>Assembling all payment-relevant fields into one object means each
 * {@link PaymentProcessor} implementation receives exactly what it needs
 * without reaching back into the {@link Order} aggregate. This also makes
 * it straightforward to unit-test processors in isolation.
 *
 * <p>Implemented as a Java record — all fields are final and a canonical
 * constructor is generated automatically.
 *
 * @param orderId     the ID of the order being paid for
 * @param customerId  the ID of the customer making the payment
 * @param amount      the exact amount to charge (post-discount total)
 * @param paymentType the method chosen by the customer
 */
public record PaymentRequest(
        UUID        orderId,
        UUID        customerId,
        BigDecimal  amount,
        PaymentType paymentType
) {

    /**
     * Convenience factory — builds a {@code PaymentRequest} directly from
     * a fully constructed {@link Order}.
     */
    public static PaymentRequest from(Order order) {
        return new PaymentRequest(
                order.getId(),
                order.getCustomer().getId(),
                order.getTotalAmount(),
                order.getPaymentType()
        );
    }
}
