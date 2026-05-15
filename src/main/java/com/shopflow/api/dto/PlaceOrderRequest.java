package com.shopflow.api.dto;

import com.shopflow.domain.model.PaymentType;
import com.shopflow.domain.model.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Inbound request object for placing a new order.
 *
 * <p>Kept in the {@code api.dto} package so that the domain and application
 * layers never depend on HTTP-level concerns. {@code OrderFacade} accepts this
 * DTO directly — it is the single entry point to the order placement flow.
 *
 * <p>Bean Validation annotations drive the first layer of input validation
 * before the facade logic even runs.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @NotNull(message = "Shipping address is required")
    @Valid
    private ShippingAddress shippingAddress;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderLineItemRequest> items;

    /** Optional delivery note. */
    private String notes;

    // ── Nested DTO ────────────────────────────────────────────────────

    /**
     * Represents a single line item within a {@link PlaceOrderRequest}.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItemRequest {

        @NotNull(message = "Product ID is required")
        private UUID productId;

        @Positive(message = "Quantity must be at least 1")
        private int quantity;
    }
}
