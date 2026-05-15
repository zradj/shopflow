package com.shopflow.domain.event;

import com.shopflow.domain.model.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Domain event fired by {@code OrderFacade} immediately after an
 * {@link Order} is persisted successfully.
 *
 * <h2>Observer Pattern (Behavioral #2)</h2>
 * <p>Spring's {@code ApplicationEventPublisher} acts as the Subject.
 * Independent listeners ({@code InventoryDeductionListener},
 * {@code NotificationListener}, {@code AnalyticsListener}) subscribe to this
 * event without the {@code OrderFacade} knowing they exist. Adding a new
 * side-effect means writing a new listener — not touching the Facade (OCP).
 */
@Getter
public class OrderPlacedEvent extends ApplicationEvent {

    private final Order  order;
    private final Instant occurredAt;

    public OrderPlacedEvent(Object source, Order order) {
        super(source);
        this.order      = order;
        this.occurredAt = Instant.now();
    }
}
