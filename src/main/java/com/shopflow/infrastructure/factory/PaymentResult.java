package com.shopflow.infrastructure.factory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable value object returned by every {@link PaymentProcessor} implementation.
 *
 * <p>Using a dedicated result type — rather than throwing exceptions for
 * failures or returning a raw boolean — means callers can inspect the
 * outcome, log a reference number, and decide on the order's next
 * {@link com.shopflow.domain.model.OrderStatus} without catching exceptions
 * for normal business flow (a common anti-pattern).
 *
 * <p>Instances are created through the two static factory methods
 * {@link #success} and {@link #failure}, which make the intent at call sites
 * immediately clear.
 */
public final class PaymentResult {

    private final boolean   successful;
    private final String    transactionReference;
    private final BigDecimal amountCharged;
    private final String    failureReason;
    private final Instant   processedAt;

    // ── Private constructor — use the static factories below ─────────

    private PaymentResult(boolean successful,
                          String transactionReference,
                          BigDecimal amountCharged,
                          String failureReason) {
        this.successful           = successful;
        this.transactionReference = transactionReference;
        this.amountCharged        = amountCharged;
        this.failureReason        = failureReason;
        this.processedAt          = Instant.now();
    }

    // ── Static factories ─────────────────────────────────────────────

    /**
     * Creates a successful payment result.
     *
     * @param transactionReference the gateway's reference ID for this charge
     * @param amountCharged        the amount actually debited
     */
    public static PaymentResult success(String transactionReference,
                                        BigDecimal amountCharged) {
        return new PaymentResult(true, transactionReference, amountCharged, null);
    }

    /**
     * Creates a failed payment result.
     *
     * @param failureReason a human-readable explanation of why the payment failed
     */
    public static PaymentResult failure(String failureReason) {
        return new PaymentResult(false, null, BigDecimal.ZERO, failureReason);
    }

    // ── Accessors ────────────────────────────────────────────────────

    public boolean   isSuccessful()           { return successful; }
    public String    getTransactionReference() { return transactionReference; }
    public BigDecimal getAmountCharged()       { return amountCharged; }
    public String    getFailureReason()        { return failureReason; }
    public Instant   getProcessedAt()          { return processedAt; }

    @Override
    public String toString() {
        if (successful) {
            return "PaymentResult{SUCCESS, ref='%s', amount=%s, at=%s}"
                    .formatted(transactionReference, amountCharged, processedAt);
        }
        return "PaymentResult{FAILURE, reason='%s', at=%s}"
                .formatted(failureReason, processedAt);
    }
}
