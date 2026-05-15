package com.shopflow.infrastructure.observer;

import com.shopflow.domain.event.OrderPlacedEvent;
import com.shopflow.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring's {@link ApplicationEventPublisher}.
 *
 * <h2>Observer Pattern — Subject (Behavioral #2)</h2>
 * <p>In the classic Observer pattern this class is the <em>Subject</em>
 * (also called the <em>Observable</em>). It does not hold a list of
 * listeners itself — Spring's event infrastructure does that. The role of
 * this class is to give the {@code OrderFacade} a single, named, domain-aware
 * method ({@link #publishOrderPlaced}) rather than forcing the facade to
 * depend on Spring's {@link ApplicationEventPublisher} directly.
 *
 * <h2>Why wrap the publisher at all?</h2>
 * <ul>
 *   <li><strong>SRP:</strong> the facade orchestrates the order flow; this
 *       class owns the event-publishing concern.</li>
 *   <li><strong>Testability:</strong> in unit tests you can mock
 *       {@code OrderEventPublisher} without wiring Spring's event bus.</li>
 *   <li><strong>Extensibility (OCP):</strong> if event publishing later needs
 *       additional behaviour — correlation IDs, outbox persistence,
 *       async dispatching — only this class changes.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher springPublisher;

    /**
     * Creates and publishes an {@link OrderPlacedEvent} to all registered
     * listeners synchronously on the calling thread.
     *
     * <p>Spring's default event dispatch is synchronous: each listener runs
     * in sequence on the same virtual thread that called this method, inside
     * the same transaction as the facade. This means a listener failure rolls
     * back the entire order — intentional for {@code InventoryDeductionListener},
     * which must remain consistent with the order commit.
     *
     * <p>To make specific listeners async (e.g. notifications), annotate them
     * with {@code @Async} — no change required here.
     *
     * @param order the fully persisted, confirmed order
     */
    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent(this, order);
        log.info("Publishing OrderPlacedEvent — orderId={}, customer={}, total={}",
                order.getId(), order.getCustomer().getEmail(), order.getTotalAmount());
        springPublisher.publishEvent(event);
    }
}
