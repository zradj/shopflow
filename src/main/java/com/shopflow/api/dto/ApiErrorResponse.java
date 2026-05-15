package com.shopflow.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope returned by {@code GlobalExceptionHandler}.
 *
 * <p>Every error response — whether a 400 validation failure or a 404
 * not-found — shares this shape, making API clients easier to write.
 *
 * <p>{@code fieldErrors} is omitted from the JSON when {@code null} so that
 * non-validation errors stay concise.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /** HTTP status code repeated in the body for client convenience. */
    private final int    status;

    /** A short, stable error code (e.g. {@code "NOT_FOUND"}). */
    private final String error;

    /** Human-readable description of what went wrong. */
    private final String message;

    /** Server timestamp at the moment the error occurred. */
    private final Instant timestamp;

    /**
     * Per-field validation errors; present only on 400 responses from
     * {@code @Valid} failures.
     */
    private final List<FieldError> fieldErrors;

    // ── Nested ───────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}
