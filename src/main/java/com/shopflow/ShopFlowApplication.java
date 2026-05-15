package com.shopflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ShopFlow application entry point.
 *
 * <p>Virtual Threads are activated via
 * {@code spring.threads.virtual.enabled=true} in {@code application.properties}.
 * Spring Boot 3.2+ automatically configures Tomcat to use a
 * virtual-thread executor for every incoming HTTP request when that property
 * is set — no code change required here.
 *
 * <p>Additional virtual-thread wiring (e.g. for {@code @Async} listeners)
 * is handled by {@link com.shopflow.config.AsyncConfig}.
 */
@SpringBootApplication
public class ShopFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopFlowApplication.class, args);
    }
}
