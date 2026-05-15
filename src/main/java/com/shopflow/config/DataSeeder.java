package com.shopflow.config;

import com.shopflow.domain.model.Customer;
import com.shopflow.domain.model.CustomerTier;
import com.shopflow.domain.model.Product;
import com.shopflow.infrastructure.repository.CustomerRepository;
import com.shopflow.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Seeds the H2 in-memory database with demo data on every startup.
 *
 * <p>Because H2 is configured with {@code ddl-auto=create-drop}, the schema
 * and all data are wiped when the application stops. This seeder ensures the
 * API is immediately usable without manual setup — useful for demos and the
 * oral defense.
 *
 * <p>In production, remove or profile-guard this bean
 * ({@code @Profile("dev")}) and use Flyway or Liquibase migrations instead.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final ProductRepository  productRepository;
    private final CustomerRepository customerRepository;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            log.info("Seeding demo data...");
            seedProducts();
            seedCustomers();
            log.info("Demo data seeded — {} products, {} customers",
                    productRepository.count(), customerRepository.count());
        };
    }

    private void seedProducts() {
        productRepository.save(Product.of(
                "Mechanical Keyboard Pro",
                "Tactile switches, RGB backlight, USB-C",
                new BigDecimal("149.99"), 50, "Electronics"));

        productRepository.save(Product.of(
                "Wireless Mouse",
                "Ergonomic, 3000 DPI, 60-hour battery",
                new BigDecimal("59.99"), 120, "Electronics"));

        productRepository.save(Product.of(
                "27\" 4K Monitor",
                "IPS panel, 144Hz, HDR400",
                new BigDecimal("499.99"), 30, "Electronics"));

        productRepository.save(Product.of(
                "Standing Desk",
                "Electric height-adjustable, 140×70 cm",
                new BigDecimal("799.00"), 15, "Furniture"));

        productRepository.save(Product.of(
                "Ergonomic Chair",
                "Lumbar support, mesh back, adjustable arms",
                new BigDecimal("349.00"), 25, "Furniture"));
    }

    private void seedCustomers() {
        customerRepository.save(Customer.of(
                "Alice Standard", "alice@example.com", CustomerTier.STANDARD));

        customerRepository.save(Customer.of(
                "Bob Premium", "bob@example.com", CustomerTier.PREMIUM));

        customerRepository.save(Customer.of(
                "Carol VIP", "carol@example.com", CustomerTier.VIP));
    }
}
