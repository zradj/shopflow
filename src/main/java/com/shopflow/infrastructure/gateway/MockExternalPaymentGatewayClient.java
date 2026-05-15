package com.shopflow.infrastructure.gateway;

import com.shopflow.infrastructure.factory.PaymentResult;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Simulates an external payment gateway over a mock HTTP boundary.
 *
 * <h2>Circuit Breaker (Resilience)</h2>
 * <p>The {@link CircuitBreaker} annotation on {@link #charge} is backed by
 * Resilience4j. When the failure rate within a sliding window exceeds the
 * configured threshold (default 50 %), the circuit <em>opens</em> and all
 * subsequent calls are immediately rejected — the gateway is not contacted at
 * all. After the configured wait duration the circuit moves to
 * <em>half-open</em>, admits a probe call, and either closes again (probe
 * succeeded) or stays open (probe failed).
 *
 * <h2>Fallback</h2>
 * <p>{@link #fallbackCharge} is invoked in two cases:
 * <ol>
 *   <li>The circuit is open ({@link CallNotPermittedException}).</li>
 *   <li>Any {@link Exception} escapes {@link #charge} (e.g. a simulated
 *       timeout).</li>
 * </ol>
 * The fallback returns a {@link PaymentResult#failure} result rather than
 * propagating an exception, keeping the order flow consistent:
 * {@code OrderFacade} always receives a {@code PaymentResult} and transitions
 * the order to {@code PAYMENT_FAILED} — no crash, no broken transaction.
 *
 * <h2>Why a mock?</h2>
 * <p>A real gateway would use {@code RestClient} or {@code WebClient} to call
 * an HTTPS endpoint. The mock replaces the network with {@link Thread#sleep}
 * (simulated latency) and a random failure roll so the circuit breaker
 * behaviour can be demonstrated and tested locally with no external dependency.
 *
 * <h2>Simulating failures for the demo</h2>
 * <p>Set {@code shopflow.gateway.failure-rate-percent=60} in
 * {@code application.properties} to push past the 50 % threshold and watch
 * the circuit open after 10 calls.
 */
@Slf4j
@Component
public class MockExternalPaymentGatewayClient implements PaymentGatewayClient {

    private static final String CIRCUIT_BREAKER_NAME = "paymentGateway";

    /** Simulated gateway round-trip latency in milliseconds. */
    private final long simulatedLatencyMs;

    /**
     * Percentage of calls that the mock will deliberately fail
     * (0 = never fail, 100 = always fail).
     */
    private final int failureRatePercent;

    private final RandomGenerator random = RandomGenerator.getDefault();

    public MockExternalPaymentGatewayClient(
            @Value("${shopflow.gateway.simulated-latency-ms:200}") long simulatedLatencyMs,
            @Value("${shopflow.gateway.failure-rate-percent:20}") int failureRatePercent) {
        this.simulatedLatencyMs  = simulatedLatencyMs;
        this.failureRatePercent  = failureRatePercent;
        log.info("MockExternalPaymentGatewayClient initialised — latency={}ms, failureRate={}%",
                simulatedLatencyMs, failureRatePercent);
    }

    // ── PaymentGatewayClient implementation ──────────────────────────

    /**
     * Executes a mock charge against the external gateway.
     *
     * <p>The method is guarded by a Resilience4j circuit breaker named
     * {@value CIRCUIT_BREAKER_NAME}. Its configuration lives in
     * {@code application.properties} under the
     * {@code resilience4j.circuitbreaker.instances.paymentGateway.*} prefix.
     *
     * <p>If the circuit is open, Resilience4j intercepts the call before this
     * method body runs and routes directly to {@link #fallbackCharge}.
     *
     * @param paymentMethod the payment method descriptor ("CREDIT_CARD" or "WALLET")
     * @param amount        the amount to charge
     * @param orderId       correlation ID logged by the gateway
     * @return a successful {@link PaymentResult} containing a transaction reference
     * @throws RuntimeException when the mock rolls a failure — triggers the
     *                          circuit breaker's failure counter and the fallback
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackCharge")
    public PaymentResult charge(String paymentMethod, BigDecimal amount, String orderId) {
        log.info("Gateway call — method={}, amount={}, orderId={}", paymentMethod, amount, orderId);

        simulateNetworkLatency();

        if (shouldFail()) {
            // Throwing here registers as a failure in Resilience4j's sliding window.
            // Once failures exceed the threshold, the circuit opens automatically.
            String msg = "Gateway timeout — payment method=%s, orderId=%s"
                    .formatted(paymentMethod, orderId);
            log.warn("Simulated gateway failure: {}", msg);
            throw new RuntimeException(msg);
        }

        String transactionReference = "TXN-" + UUID.randomUUID().toString().toUpperCase();
        log.info("Gateway success — orderId={}, ref={}", orderId, transactionReference);
        return PaymentResult.success(transactionReference, amount);
    }

    // ── Fallback ─────────────────────────────────────────────────────

    /**
     * Invoked by Resilience4j when the circuit is open or when
     * {@link #charge} throws any {@link Exception}.
     *
     * <p><strong>Signature rule:</strong> the fallback method must have the
     * same parameters as the guarded method, plus an additional
     * {@link Throwable} (or a specific exception type) as the last parameter.
     * Resilience4j matches fallback methods by name and signature at startup.
     *
     * @param paymentMethod forwarded from the original call
     * @param amount        forwarded from the original call
     * @param orderId       forwarded from the original call
     * @param throwable     the exception that caused the fallback (may be
     *                      {@link CallNotPermittedException} when open)
     * @return a failure {@link PaymentResult} marking the order for manual review
     */
    private PaymentResult fallbackCharge(String paymentMethod,
                                         BigDecimal amount,
                                         String orderId,
                                         Throwable throwable) {
        boolean circuitOpen = throwable instanceof CallNotPermittedException;

        if (circuitOpen) {
            log.error("Circuit OPEN — payment gateway unavailable. orderId={}", orderId);
        } else {
            log.error("Gateway call failed, fallback triggered. orderId={}, cause={}",
                    orderId, throwable.getMessage());
        }

        String reason = circuitOpen
                ? "Payment gateway is currently unavailable (circuit open). " +
                  "Order marked for manual payment review."
                : "Payment gateway returned an error: " + throwable.getMessage();

        return PaymentResult.failure(reason);
    }

    // ── Private helpers ───────────────────────────────────────────────

    /**
     * Blocks the current thread for {@link #simulatedLatencyMs} milliseconds
     * to model a real network round-trip.
     *
     * <p>On Java 21 virtual threads this is a non-issue: the virtual thread
     * is unmounted from its carrier during the sleep, making the carrier
     * available for other virtual threads. The same sleep on a platform thread
     * would block the entire OS thread.
     */
    private void simulateNetworkLatency() {
        try {
            Thread.sleep(simulatedLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gateway call interrupted", e);
        }
    }

    /**
     * Returns {@code true} with probability {@link #failureRatePercent} / 100.
     */
    private boolean shouldFail() {
        return random.nextInt(100) < failureRatePercent;
    }
}
