# Refactoring Case Study

Before applying design patterns, the `OrderController` looked like this (simplified):

```java
// ❌ LEGACY — everything in one place
@PostMapping("/orders")
public Order placeOrder(@RequestBody Map<String, Object> body) {
    // Fetch customer directly from repo
    Customer customer = customerRepository.findById(...)
        .orElseThrow(...);

    // Fetch products directly from repo
    List<OrderItem> items = new ArrayList<>();
    for (var item : (List) body.get("items")) {
        Product p = productRepository.findById(...).orElseThrow(...);
        items.add(new OrderItem(p, (int) item.get("quantity")));
    }

    // Discount logic inline — must edit this class to change rates
    BigDecimal discount = BigDecimal.ZERO;
    if (customer.getTier().equals("PREMIUM")) {
        discount = subtotal.multiply(new BigDecimal("0.10"));
    } else if (customer.getTier().equals("VIP")) {
        discount = subtotal.multiply(new BigDecimal("0.20"));
    }

    // Build order directly in controller
    Order order = new Order();
    order.setCustomer(customer);
    order.setItems(items);
    order.setDiscountAmount(discount);
    order.setStatus("PENDING");
    order.setPaymentType(body.get("paymentType").toString());
    orderRepository.save(order);

    // Payment logic inline — coupled to a specific provider
    try {
        String ref = restTemplate.postForObject(PAYMENT_URL, body, String.class);
        order.setStatus("CONFIRMED");
    } catch (Exception e) {
        order.setStatus("FAILED");
    }

    // Stock update inline
    for (OrderItem item : items) {
        item.getProduct().setStockQuantity(
            item.getProduct().getStockQuantity() - item.getQuantity()
        );
        productRepository.save(item.getProduct());
    }

    // Notification inline
    emailService.send(customer.getEmail(), "Order placed: " + order.getId());

    return order;
}
```

**Problems identified:**

| Problem | Violated principle |
|---|---|
| Controller fetches from repositories directly | DIP — depends on concrete classes |
| Discount `if/else` chain in the controller | OCP — must edit this class to add a tier |
| `new Order()` + setters scattered everywhere | No invariant enforcement; invalid states possible |
| Payment URL hardcoded; no resilience | No circuit breaker; one timeout crashes the request |
| Stock deduction and email inside the controller | SRP — the controller has 5+ reasons to change |

### The Refactoring Steps

**Step 1 — Extract discount logic (Strategy pattern)**
The `if/else` chain became `DiscountStrategy` implementations. `DiscountStrategyFactory` selects the right one at runtime. The controller no longer contains any pricing logic.

**Step 2 — Enforce construction invariants (Builder pattern)**
`new Order()` + setters was replaced with `Order.Builder`. The private constructor means an `Order` can only exist in a valid state. An empty items list throws at `build()` time, not silently at query time.

**Step 3 — Decouple payment from the controller (Factory Method + Circuit Breaker)**
`PaymentProcessorFactory` selects the right processor. The processor delegates to `PaymentGatewayClient`. `MockExternalPaymentGatewayClient` adds `@CircuitBreaker` at the infrastructure boundary. The controller imports none of this.

**Step 4 — Decouple side-effects (Observer pattern)**
Stock deduction, email, and analytics were each extracted into an `@EventListener`. `OrderFacade` publishes one event — it does not know how many listeners respond or what they do.

**Step 5 — Hide orchestration behind a Facade**
All of steps 1-4 were assembled into `OrderFacade.placeOrder()`. The controller became:

```java
// ✅ REFACTORED — controller is 3 lines
@PostMapping
public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest req) {
    return ResponseEntity.status(CREATED).body(OrderResponse.from(orderFacade.placeOrder(req)));
}
```

The controller now has exactly one reason to change: if the HTTP contract changes.