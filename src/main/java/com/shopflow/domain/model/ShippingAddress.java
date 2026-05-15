package com.shopflow.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Value object embedded directly inside the {@code orders} table.
 *
 * <p>Extracting address into its own class honours SRP — the {@code Order}
 * entity is not responsible for knowing what constitutes a valid address.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    @NotBlank
    private String street;

    @NotBlank
    private String city;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String country;

    @Override
    public String toString() {
        return "%s, %s %s, %s".formatted(street, city, postalCode, country);
    }
}
