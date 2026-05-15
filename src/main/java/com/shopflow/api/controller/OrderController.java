package com.shopflow.api.controller;

import com.shopflow.api.dto.OrderResponse;
import com.shopflow.api.dto.PlaceOrderRequest;
import com.shopflow.application.facade.OrderFacade;
import com.shopflow.application.service.OrderService;
import com.shopflow.domain.model.Order;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Exposes order placement and retrieval endpoints.
 *
 * <h2>Facade Pattern in action</h2>
 * <p>The {@code POST /api/orders} handler is intentionally three lines long.
 * All orchestration — stock checks, discount calculation, payment processing,
 * event publishing — happens inside {@link OrderFacade#placeOrder}. The
 * controller's only job is HTTP: parse the request, call the facade, map to a
 * DTO, and set the status code.
 *
 * <h2>Separation of reads and writes</h2>
 * <p>Write operations go through {@link OrderFacade} (which owns the full
 * transactional flow). Read operations go directly to {@link OrderService},
 * bypassing the facade entirely — there is nothing to orchestrate for a
 * simple lookup.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade   orderFacade;
    private final OrderService  orderService;

    /**
     * Places a new order end-to-end.
     *
     * <p>The request body is validated by Bean Validation before the method
     * body runs. Any violation produces a 400 from {@code GlobalExceptionHandler}.
     *
     * @param request the validated order request
     * @return 201 Created with the resulting order, or an appropriate error
     *         response if stock, payment, or validation fails
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        log.info("POST /api/orders — customerId={}", request.getCustomerId());
        Order order = orderFacade.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(OrderResponse.from(order));
    }

    /**
     * Retrieves a single order by its ID, including all line items.
     *
     * @param id the order UUID
     * @return 200 with the order, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        log.debug("GET /api/orders/{}", id);
        return ResponseEntity.ok(OrderResponse.from(orderFacade.getOrder(id)));
    }

    /**
     * Returns all orders placed by the specified customer.
     *
     * @param customerId the customer's UUID
     * @return 200 with the order list (may be empty)
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable UUID customerId) {
        log.debug("GET /api/orders/customer/{}", customerId);
        List<OrderResponse> orders = orderService.findByCustomerId(customerId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }
}
