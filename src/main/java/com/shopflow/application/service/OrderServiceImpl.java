package com.shopflow.application.service;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.OrderStatus;
import com.shopflow.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Database-backed implementation of {@link OrderService}.
 *
 * <h2>Single Responsibility</h2>
 * <p>This class does exactly one thing: translate {@link OrderService} calls
 * into {@link com.shopflow.infrastructure.repository.OrderRepository}
 * operations. It has no awareness of payment processing, discounts, caching,
 * or event publishing — those concerns belong to {@code OrderFacade} and
 * other collaborating services.
 *
 * <h2>Transaction strategy</h2>
 * <p>The class-level {@code @Transactional(readOnly = true)} applies to all
 * query methods. The two write methods ({@link #save} and
 * {@link #updateStatus}) override this with a full read-write transaction.
 * This is a standard Spring pattern: optimise for reads by default, be
 * explicit about writes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    // ── OrderService implementation ───────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>The {@code Order} is expected to have been assembled by
     * {@code Order.Builder} inside {@code OrderFacade} before being passed
     * here. {@code OrderService} does not know how orders are constructed —
     * it only persists them.
     */
    @Override
    @Transactional
    public Order save(Order order) {
        Order saved = orderRepository.save(order);
        log.info("Order persisted — id={}, customer={}, total={}, status={}",
                saved.getId(),
                saved.getCustomer().getEmail(),
                saved.getTotalAmount(),
                saved.getStatus());
        return saved;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses the {@code findByIdWithItems} JPQL query to load the order
     * together with its line items and products in one round trip,
     * preventing lazy-loading N+1 issues when the caller iterates items.
     */
    @Override
    public Order findById(UUID id) {
        log.debug("Fetching order by id={}", id);
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        log.debug("Fetching orders for customerId={}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Order> findByStatus(OrderStatus status) {
        log.debug("Fetching orders with status={}", status);
        return orderRepository.findByStatus(status);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates the actual state change to {@link Order#transitionTo},
     * keeping transition logic inside the domain aggregate where it belongs
     * (SRP). This method is responsible only for loading, triggering the
     * transition, and persisting the result.
     */
    @Override
    @Transactional
    public Order updateStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus previous = order.getStatus();
        order.transitionTo(newStatus);
        Order saved = orderRepository.save(order);

        log.info("Order {} status: {} → {}", orderId, previous, newStatus);
        return saved;
    }
}
