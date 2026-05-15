package com.shopflow.api.dto;

import com.shopflow.domain.model.Customer;
import com.shopflow.domain.model.CustomerTier;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound DTO for {@link Customer}.
 *
 * <p>Controllers never return JPA entities directly. This DTO is the contract
 * between the backend and its callers. Mapping is handled by the
 * {@link #from(Customer)} static factory so that no mapping logic leaks into
 * the controller.
 */
@Getter
@Builder
public class CustomerResponse {

    private final UUID         id;
    private final String       name;
    private final String       email;
    private final CustomerTier tier;
    private final Instant      createdAt;
    private final Instant      updatedAt;

    /**
     * Maps a {@link Customer} entity to a {@link CustomerResponse}.
     *
     * @param customer must not be {@code null}
     */
    public static CustomerResponse from(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .tier(customer.getTier())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
