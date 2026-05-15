package com.shopflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Enables asynchronous event listener dispatch and wires the executor
 * to Java 21 Virtual Threads.
 *
 * <h2>Why a virtual-thread executor?</h2>
 * <p>Spring's default {@code @Async} executor is a thread-pool backed by
 * platform threads. Replacing it with a virtual-thread executor means every
 * async listener ({@code NotificationListener}, {@code AnalyticsListener})
 * runs on a lightweight virtual thread rather than borrowing a pooled
 * platform thread. Under high order volume this removes a significant
 * bottleneck — virtual threads are cheap enough to create per-task rather
 * than pooling them.
 *
 * <h2>Naming</h2>
 * <p>The bean is named {@code "taskExecutor"}, which is the name Spring
 * looks for when resolving the default executor for {@code @Async} methods.
 * No additional configuration on {@code @Async} annotations is needed.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Provides a virtual-thread-per-task executor as the default
     * {@code @Async} executor for the application.
     *
     * <p>{@code Executors.newVirtualThreadPerTaskExecutor()} creates a new
     * virtual thread for every submitted task and never pools them — the JVM
     * scheduler handles multiplexing onto carrier threads automatically.
     *
     * @return a virtual-thread executor named {@code "taskExecutor"}
     */
    @Bean(name = "taskExecutor")
    public Executor virtualThreadAsyncExecutor() {
        log.info("Configuring @Async executor — Java 21 virtual threads");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
