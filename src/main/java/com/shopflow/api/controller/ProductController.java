package com.shopflow.api.controller;

import com.shopflow.api.dto.ProductResponse;
import com.shopflow.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Exposes read-only product catalogue endpoints.
 *
 * <h2>Responsibilities (SRP)</h2>
 * <p>This controller does exactly three things: parse the HTTP request,
 * call the service, and map the result to a DTO. No business logic,
 * no direct repository access, no caching concerns — those belong to
 * {@code CachingProductService} and {@code ProductServiceImpl} respectively.
 *
 * <h2>Virtual Threads</h2>
 * <p>With {@code spring.threads.virtual.enabled=true} in
 * {@code application.properties}, every request to this controller is
 * handled on a Java 21 virtual thread automatically — no code change needed.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    /**
     * Spring injects {@code CachingProductService} here (it is marked
     * {@code @Primary}) — the controller never names a concrete class.
     */
    private final ProductService productService;

    /**
     * Returns every product in the catalogue.
     *
     * <p>The response is served from the Redis cache on repeated calls
     * (key {@code "product::all"}), falling back to H2 on a miss.
     *
     * @return 200 with the full product list (may be empty)
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.debug("GET /api/products");
        List<ProductResponse> products = productService.findAll().stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    /**
     * Returns a single product by its UUID.
     *
     * <p>Cached under key {@code "product::{id}"}.
     *
     * @param id product UUID from the path
     * @return 200 with the product, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        log.debug("GET /api/products/{}", id);
        return ResponseEntity.ok(ProductResponse.from(productService.findById(id)));
    }

    /**
     * Returns all products in the specified category.
     *
     * <p>Cached under key {@code "product::category::{category}"}.
     *
     * @param category case-sensitive category name
     * @return 200 with the matching products (may be empty)
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getByCategory(
            @PathVariable String category) {
        log.debug("GET /api/products/category/{}", category);
        List<ProductResponse> products = productService.findByCategory(category).stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    /**
     * Returns only products that currently have stock available.
     *
     * <p>Cached under key {@code "product::in_stock"}.
     *
     * @return 200 with in-stock products (may be empty)
     */
    @GetMapping("/in-stock")
    public ResponseEntity<List<ProductResponse>> getInStock() {
        log.debug("GET /api/products/in-stock");
        List<ProductResponse> products = productService.findInStock().stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }
}
