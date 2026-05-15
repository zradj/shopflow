package com.shopflow.application.facade;

import com.shopflow.api.dto.PlaceOrderRequest;
import com.shopflow.application.service.*;
import com.shopflow.application.strategy.DiscountStrategy;
import com.shopflow.application.strategy.DiscountStrategyFactory;
import com.shopflow.domain.model.*;
import com.shopflow.infrastructure.factory.PaymentProcessorFactory;
import com.shopflow.infrastructure.factory.PaymentResult;
import com.shopflow.infrastructure.observer.OrderEventPublisher;
import com.shopflow.infrastructure.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Orchestrates the complete order placement flow.
 *
 * <h2>Facade Pattern (Structural #1)</h2>
 * <p>This class is the single public entry point for placing an order.
 * The {@code OrderController} calls {@link #placeOrder} and receives a
 * finished {@link Order} — it has no knowledge of how stock is validated,
 * which discount applies, how payment is processed, or which side-effects
 * are triggered afterward. All of that complexity is hidden behind this
 * one method.
 *
 * <h2>Order placement sequence</h2>
 * <ol>
 *   <li>Resolve the {@link Customer} from the request.</li>
 *   <li>Resolve each {@link Product} and add it to {@code Order.Builder}
 *       (Builder pattern).</li>
 *   <li>Select the {@link DiscountStrategy} for the customer's tier
 *       (Strategy pattern) and apply it.</li>
 *   <li>Build and persist the {@link Order} via {@link OrderService}.</li>
 *   <li>Validate stock via {@link InventoryService}.</li>
 *   <li>Process payment via {@link PaymentProcessorFactory}
 *       (Factory Method pattern).</li>
 *   <li>Transition the order status based on the payment outcome.</li>
 *   <li>Deduct stock on success.</li>
 *   <li>Publish {@link OrderPlacedEvent} (Observer pattern).</li>
 * </ol>
 *
 * <h2>Transaction boundary</h2>
 * <p>The entire flow runs inside one transaction. If payment succeeds but
 * stock deduction fails (an unlikely but possible race), the transaction
 * rolls back, leaving the database consistent. A real system would also
 * issue a payment reversal at that point — flagged here as a known
 * extension.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final CustomerRepository      customerRepository;
    private final ProductService          productService;
    private final InventoryService        inventoryService;
    private final OrderService            orderService;
    private final DiscountStrategyFactory discountStrategyFactory;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final OrderEventPublisher     orderEventPublisher;

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Places an order end-to-end.
     *
     * @param request the validated inbound request from the controller
     * @return the persisted, status-updated {@link Order}
     * @throws CustomerNotFoundException      if the customer does not exist
     * @throws ProductNotFoundException       if any requested product does not exist
     * @throws InsufficientStockException     if stock cannot fulfil the request
     */
    @Transactional
    public Order placeOrder(PlaceOrderRequest request) {
        log.info("Order placement started — customerId={}, items={}",
                request.getCustomerId(), request.getItems().size());

        // ── Step 1: Resolve customer ─────────────────────────────────
        Customer customer = resolveCustomer(request.getCustomerId());

        // ── Step 2: Build the order (Builder pattern) ────────────────
        Order.Builder builder = new Order.Builder(
                customer,
                request.getPaymentType(),
                request.getShippingAddress()
        );

        for (PlaceOrderRequest.OrderLineItemRequest lineItem : request.getItems()) {
            Product product = productService.findById(lineItem.getProductId());
            builder.addItem(product, lineItem.getQuantity());
        }

        if (request.getNotes() != null) {
            builder.notes(request.getNotes());
        }

        // ── Step 3: Apply discount (Strategy pattern) ────────────────
        DiscountStrategy strategy = discountStrategyFactory.getStrategy(customer.getTier());
        BigDecimal subtotal = builder.getCurrentSubtotal();
        BigDecimal discount = strategy.calculate(subtotal);

        log.info("Discount applied — strategy='{}', subtotal={}, discount={}",
                strategy.describe(), subtotal, discount);

        builder.discountAmount(discount);

        // ── Step 4: Persist the order (PENDING) ──────────────────────
        Order order = orderService.save(builder.build());
        log.info("Order persisted — id={}, total={}", order.getId(), order.getTotalAmount());

        // ── Step 5: Validate stock ───────────────────────────────────
        inventoryService.validateStock(order);

        // ── Step 6: Process payment (Factory Method pattern) ─────────
        PaymentResult paymentResult = paymentProcessorFactory
                .getProcessor(request.getPaymentType())
                .process(order);

        // ── Step 7: Transition order status ──────────────────────────
        if (paymentResult.isSuccessful()) {
            order = orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED);
            log.info("Payment confirmed — order={}, ref={}",
                    order.getId(), paymentResult.getTransactionReference());

            // ── Step 8: Deduct stock ─────────────────────────────────
            inventoryService.deductStock(order);

            // ── Step 9: Publish domain event (Observer pattern) ──────
            orderEventPublisher.publishOrderPlaced(order);
            log.info("OrderPlacedEvent published for order={}", order.getId());

        } else {
            order = orderService.updateStatus(order.getId(), OrderStatus.PAYMENT_FAILED);
            log.warn("Payment failed — order={}, reason={}",
                    order.getId(), paymentResult.getFailureReason());
        }

        return order;
    }

    /**
     * Retrieves a full order by ID, including all line items.
     * Delegates directly to {@link OrderService} — no orchestration needed.
     *
     * @param orderId the order UUID
     * @return the matching order
     * @throws OrderNotFoundException if no order exists with that id
     */
    public Order getOrder(UUID orderId) {
        return orderService.findById(orderId);
    }

    // ── Private helpers ───────────────────────────────────────────────

    private Customer resolveCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
