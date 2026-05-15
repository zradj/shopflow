package com.shopflow.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a registered customer.
 *
 * <p>The {@link #tier} field is the primary input consumed by the
 * {@code DiscountStrategy} — no discount logic lives here (SRP).
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Loyalty tier. Determines which {@code DiscountStrategy} implementation
     * the {@code OrderFacade} selects at order-placement time.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerTier tier = CustomerTier.STANDARD;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Factory method for convenience in tests & seeders ────────────

    public static Customer of(String name, String email, CustomerTier tier) {
        Customer c = new Customer();
        c.name  = name;
        c.email = email;
        c.tier  = tier;
        return c;
    }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", email='" + email + "', tier=" + tier + "}";
    }
}
