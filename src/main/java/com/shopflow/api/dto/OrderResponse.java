package com.shopflow.api.dto;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.OrderItem;
import com.shopflow.domain.model.OrderStatus;
import com.shopflow.domain.model.PaymentType;
import com.shopflow.domain.model.ShippingAddress;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbound DTO for {@link Order}.
 *
 * <p>Includes a nested {@link OrderItemResponse} for each line item so the
 * caller receives a fully self-contained response without needing to make
 * additional requests.
 */
@Getter
@Builder
public class OrderResponse {

    private final UUID            id;
    private final UUID            customerId;
    private final String          customerEmail;
    private final List<OrderItemResponse> items;
    private final BigDecimal      subtotal;
    private final BigDecimal      discountAmount;
    private final BigDecimal      totalAmount;
    private final OrderStatus     status;
    private final PaymentType     paymentType;
    private final ShippingAddress shippingAddress;
    private final String          notes;
    private final Instant         placedAt;
    private final Instant         updatedAt;

    /**
     * Maps an {@link Order} entity (with its items loaded) to an
     * {@link OrderResponse}.
     *
     * @param order must not be {@code null}; items must be initialised
     */
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemDtos = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerEmail(order.getCustomer().getEmail())
                .items(itemDtos)
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentType(order.getPaymentType())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .placedAt(order.getPlacedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    // ── Nested DTO ────────────────────────────────────────────────────

    /**
     * Represents a single line item within an {@link OrderResponse}.
     */
    @Getter
    @Builder
    public static class OrderItemResponse {

        private final UUID       id;
        private final UUID       productId;
        private final String     productName;
        private final int        quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;

        public static OrderItemResponse from(OrderItem item) {
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getSubtotal())
                    .build();
        }
    }
}
