package com.shopflow.application.service;

import com.shopflow.api.dto.CreateCustomerRequest;
import com.shopflow.domain.model.Customer;
import com.shopflow.infrastructure.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Database-backed implementation of {@link CustomerService}.
 *
 * <h2>Single Responsibility</h2>
 * <p>This class has exactly one job: translate {@link CustomerService} calls
 * into JPA repository operations. It has no knowledge of HTTP or caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    /** {@inheritDoc} */
    @Override
    public List<Customer> findAll() {
        log.debug("DB query — findAll customers");
        return customerRepository.findAll();
    }

    /** {@inheritDoc} */
    @Override
    public Customer findById(UUID id) {
        log.debug("DB query — findById customer({})", id);
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Customer create(CreateCustomerRequest request) {
        log.debug("Creating customer with email={}", request.getEmail());

        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException(
                    "A customer with email '" + request.getEmail() + "' already exists");
        }

        Customer customer = Customer.of(request.getName(), request.getEmail(), request.getTier());
        Customer saved = customerRepository.save(customer);
        log.info("Created customer id={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }
}
