package com.shopflow.infrastructure.observer;

import com.shopflow.application.service.InventoryService;
import com.shopflow.domain.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Deducts stock for every line item in a confirmed order.
 *
 * <h2>Observer Pattern — Concrete Observer #1</h2>
 * <p>This listener is one of three independent subscribers to
 * {@link OrderPlacedEvent}. It has no knowledge of the other listeners, and
 * {@code OrderFacade} has no knowledge of this class — the event bus is the
 * only coupling between them.
 *
 * <h2>Ordering</h2>
 * <p>{@code @Order(1)} ensures stock deduction runs first, before notification
 * or analytics. If deduction throws (e.g. a race condition caused stock to
 * reach zero between validation and this point), the transaction rolls back
 * before the notification listener fires — preventing a customer from
 * receiving a confirmation email for an order that was never actually fulfilled.
 *
 * <h2>Transaction participation</h2>
 * <p>Because Spring's default event dispatch is synchronous, this listener
 * runs inside the same transaction opened by {@code OrderFacade.placeOrder}.
 * A failure here rolls back the entire order commit — the correct behaviour
 * for an operation that must stay consistent with the order record.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class InventoryDeductionListener {

    private final InventoryService inventoryService;

    /**
     * Responds to {@link OrderPlacedEvent} by deducting stock for each item.
     *
     * @param event the event carrying the confirmed order
     */
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        com.shopflow.domain.model.Order order = event.getOrder();
        log.info("[Observer] InventoryDeductionListener — orderId={}, items={}",
                order.getId(), order.getItems().size());

        inventoryService.deductStock(order);

        log.info("[Observer] InventoryDeductionListener complete — orderId={}", order.getId());
    }
}
