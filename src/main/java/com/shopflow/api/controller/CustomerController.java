package com.shopflow.api.controller;

import com.shopflow.api.dto.CreateCustomerRequest;
import com.shopflow.api.dto.CustomerResponse;
import com.shopflow.application.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Exposes customer management endpoints.
 *
 * <h2>Responsibilities (SRP)</h2>
 * <p>This controller does exactly three things: parse the HTTP request,
 * call the service, and map the result to a DTO. No business logic or
 * direct repository access.
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Returns every registered customer.
     *
     * @return 200 with the full customer list (may be empty)
     */
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        log.debug("GET /api/customers");
        List<CustomerResponse> customers = customerService.findAll().stream()
                .map(CustomerResponse::from)
                .toList();
        return ResponseEntity.ok(customers);
    }

    /**
     * Retrieves a single customer by their UUID.
     *
     * @param id customer UUID from the path
     * @return 200 with the customer, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable UUID id) {
        log.debug("GET /api/customers/{}", id);
        return ResponseEntity.ok(CustomerResponse.from(customerService.findById(id)));
    }

    /**
     * Creates a new customer.
     *
     * <p>The request body is validated by Bean Validation before the method
     * body runs. Any violation produces a 400 from {@code GlobalExceptionHandler}.
     * A duplicate email produces a 400 with a descriptive message.
     *
     * @param request the validated creation request
     * @return 201 Created with the new customer
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        log.info("POST /api/customers — email={}", request.getEmail());
        CustomerResponse response = CustomerResponse.from(customerService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
