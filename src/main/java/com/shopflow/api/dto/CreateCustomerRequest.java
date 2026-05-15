package com.shopflow.api.dto;

import com.shopflow.domain.model.CustomerTier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Inbound request object for creating a new customer.
 *
 * <p>Bean Validation annotations drive the first layer of input validation
 * before any service logic runs. Validation errors are handled globally by
 * {@code GlobalExceptionHandler} and returned as a structured 400 response.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    /**
     * Loyalty tier for the new customer. Defaults to {@link CustomerTier#STANDARD}
     * when omitted from the request body.
     */
    @NotNull(message = "Tier is required")
    @Builder.Default
    private CustomerTier tier = CustomerTier.STANDARD;
}
