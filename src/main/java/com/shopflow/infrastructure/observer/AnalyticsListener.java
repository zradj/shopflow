package com.shopflow.infrastructure.observer;

import com.shopflow.domain.event.OrderPlacedEvent;
import com.shopflow.domain.model.CustomerTier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Records order metrics for reporting and monitoring purposes.
 *
 * <h2>Observer Pattern — Concrete Observer #3</h2>
 * <p>This listener is completely decoupled from {@code OrderFacade} and from
 * the other two listeners. It tracks per-product order counts, revenue
 * totals, and a breakdown by customer tier — the kind of data a business
 * dashboard would consume. In production this would write to a time-series
 * database (InfluxDB, CloudWatch) or publish to a Kafka topic; here it uses
 * in-memory {@link java.util.concurrent.atomic} counters so the metrics are
 * visible without external infrastructure.
 *
 * <h2>Thread safety</h2>
 * <p>All mutable state uses {@link ConcurrentHashMap} and {@link AtomicLong}/
 * {@link AtomicInteger} so concurrent virtual threads updating metrics
 * simultaneously produce correct totals without synchronised blocks.
 *
 * <h2>Async execution</h2>
 * <p>{@code @Async} means analytics recording never slows down the HTTP
 * response. Losing a metric on a restart is acceptable; losing an order is
 * not — this is the correct asymmetry.
 *
 * <h2>Ordering</h2>
 * <p>{@code @Order(3)} — analytics runs last, after stock deduction and
 * notifications.
 */
@Slf4j
@Component
@Order(3)
public class AnalyticsListener {

    // ── In-memory metric stores (thread-safe) ─────────────────────

    /** Total number of confirmed orders placed. */
    private final AtomicInteger totalOrdersPlaced = new AtomicInteger(0);

    /** Cumulative revenue across all confirmed orders, stored in cents to avoid floating-point. */
    private final AtomicLong totalRevenueCents = new AtomicLong(0L);

    /** Per-product order count: productId → number of orders containing that product. */
    private final ConcurrentHashMap<UUID, AtomicInteger> orderCountByProduct =
            new ConcurrentHashMap<>();

    /** Order count broken down by customer tier. */
    private final ConcurrentHashMap<CustomerTier, AtomicInteger> orderCountByTier =
            new ConcurrentHashMap<>();

    // ── Listener ─────────────────────────────────────────────────

    /**
     * Responds to {@link OrderPlacedEvent} by recording order metrics.
     *
     * @param event the event carrying the confirmed order
     */
    @Async
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        com.shopflow.domain.model.Order order = event.getOrder();

        // ── Total order counter ───────────────────────────────────
        int orderCount = totalOrdersPlaced.incrementAndGet();

        // ── Revenue accumulation (BigDecimal → cents to stay integer) ─
        long revenueCents = order.getTotalAmount()
                .movePointRight(2)
                .longValue();
        long cumulativeRevenue = totalRevenueCents.addAndGet(revenueCents);

        // ── Per-product counts ────────────────────────────────────
        order.getItems().forEach(item -> {
            UUID productId = item.getProduct().getId();
            orderCountByProduct
                    .computeIfAbsent(productId, id -> new AtomicInteger(0))
                    .incrementAndGet();
        });

        // ── Per-tier counts ───────────────────────────────────────
        CustomerTier tier = order.getCustomer().getTier();
        orderCountByTier
                .computeIfAbsent(tier, t -> new AtomicInteger(0))
                .incrementAndGet();

        log.info("""
                [Observer] AnalyticsListener — metrics snapshot
                ┌─────────────────────────────────────────────
                │ Event:            OrderPlaced #{}
                │ Total orders:     {}
                │ Cumulative rev:   {} (cents)
                │ Customer tier:    {}
                │ Tier order count: {}
                └─────────────────────────────────────────────""",
                order.getId(),
                orderCount,
                cumulativeRevenue,
                tier,
                orderCountByTier.get(tier).get());
    }

    // ── Read-only accessors (for a future /actuator/metrics endpoint) ─

    public int getTotalOrdersPlaced() {
        return totalOrdersPlaced.get();
    }

    public long getTotalRevenueCents() {
        return totalRevenueCents.get();
    }

    public Map<UUID, AtomicInteger> getOrderCountByProduct() {
        return Map.copyOf(orderCountByProduct);
    }

    public Map<CustomerTier, AtomicInteger> getOrderCountByTier() {
        return Map.copyOf(orderCountByTier);
    }
}
