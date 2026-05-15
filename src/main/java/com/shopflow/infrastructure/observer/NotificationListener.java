package com.shopflow.infrastructure.observer;

import com.shopflow.domain.event.OrderPlacedEvent;
import com.shopflow.domain.model.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Simulates sending an order confirmation notification to the customer.
 *
 * <h2>Observer Pattern — Concrete Observer #2</h2>
 * <p>In a production system this listener would call an email service,
 * SMS gateway, or push notification provider. Here it logs a structured
 * confirmation message — the integration point is the same regardless of
 * the channel, keeping the observer contract clean.
 *
 * <h2>Async execution</h2>
 * <p>{@code @Async} tells Spring to dispatch this listener on a separate
 * thread from the one that published the event. This means:
 * <ul>
 *   <li>The {@code OrderFacade} transaction commits and the HTTP response
 *       returns to the client without waiting for notification delivery.</li>
 *   <li>A transient email-service failure does not roll back the order.</li>
 * </ul>
 * This is the right trade-off for notifications: eventual delivery is
 * acceptable; blocking the order commit is not.
 *
 * <p>To activate {@code @Async}, {@code @EnableAsync} must be present on a
 * configuration class — see {@code AsyncConfig}.
 *
 * <h2>Ordering</h2>
 * <p>{@code @Order(2)} places this after {@link InventoryDeductionListener}
 * (order 1). In practice the async dispatch means ordering is approximate
 * for this listener, but declaring it documents the intended sequence.
 */
@Slf4j
@Component
@Order(2)
public class NotificationListener {

    /**
     * Responds to {@link OrderPlacedEvent} by dispatching a simulated
     * order confirmation to the customer asynchronously.
     *
     * @param event the event carrying the confirmed order
     */
    @Async
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        com.shopflow.domain.model.Order order = event.getOrder();

        // Build a readable item summary for the "email body"
        StringBuilder itemSummary = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemSummary.append("\n  - %s × %d @ %s each"
                    .formatted(item.getProduct().getName(),
                               item.getQuantity(),
                               item.getUnitPrice()));
        }

        log.info("""
                [Observer] NotificationListener — simulated confirmation email
                ┌─────────────────────────────────────────────
                │ To:        {}
                │ Subject:   Order Confirmation #{}
                │ Items:     {}
                │ Subtotal:  {}
                │ Discount:  {}
                │ Total:     {}
                │ Status:    {}
                │ Ship to:   {}
                └─────────────────────────────────────────────""",
                order.getCustomer().getEmail(),
                order.getId(),
                itemSummary,
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress());
    }
}
