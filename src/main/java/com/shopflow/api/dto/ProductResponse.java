package com.shopflow.api.dto;

import com.shopflow.domain.model.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound DTO for {@link Product}.
 *
 * <p>Controllers never return JPA entities directly — doing so would expose
 * Hibernate internals, risk lazy-loading exceptions after the session closes,
 * and couple the HTTP API to the database schema. This DTO is the contract
 * between the backend and its callers.
 *
 * <p>Mapping is handled by the {@link #from(Product)} static factory so
 * that no mapping logic leaks into the controller.
 */
@Getter
@Builder
public class ProductResponse {

    private final UUID       id;
    private final String     name;
    private final String     description;
    private final BigDecimal price;
    private final int        stockQuantity;
    private final String     category;
    private final Instant    createdAt;
    private final Instant    updatedAt;

    /**
     * Maps a {@link Product} entity to a {@link ProductResponse}.
     *
     * @param product must not be {@code null}
     */
    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
