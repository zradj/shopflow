package com.shopflow.infrastructure.repository;

import com.shopflow.domain.model.Order;
import com.shopflow.domain.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Order}.
 *
 * <p>Kept in the {@code infrastructure} layer. Application services depend
 * on this interface, never on its generated implementation (DIP).
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Returns all orders placed by a specific customer. */
    List<Order> findByCustomerId(UUID customerId);

    /** Returns all orders currently in the given status. */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Fetches an order together with its items and their products in a
     * single query, avoiding N+1 selects when the full order detail is needed.
     */
    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.items i
            JOIN FETCH i.product
            WHERE o.id = :id
            """)
    java.util.Optional<Order> findByIdWithItems(@Param("id") UUID id);
}
