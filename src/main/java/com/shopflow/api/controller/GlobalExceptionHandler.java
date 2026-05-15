package com.shopflow.api.controller;

import com.shopflow.api.dto.ApiErrorResponse;
import com.shopflow.application.service.*;
import com.shopflow.infrastructure.factory.PaymentProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Centralises exception-to-HTTP-response mapping for the entire API.
 *
 * <h2>Single Responsibility</h2>
 * <p>Controllers throw or let domain exceptions propagate — they never
 * construct error responses themselves. All that logic lives here and
 * nowhere else.
 *
 * <h2>Open/Closed Principle</h2>
 * <p>Adding a new exception type means adding a new {@code @ExceptionHandler}
 * method. No existing handler is modified.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 Bad Request ───────────────────────────────────────────────

    /**
     * Handles {@code @Valid} / {@code @Validated} failures on request bodies.
     * Returns the full list of per-field violations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        BindingResult br = ex.getBindingResult();
        List<ApiErrorResponse.FieldError> fieldErrors = br.getFieldErrors().stream()
                .map(fe -> ApiErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        log.warn("Validation failed: {}", fieldErrors);

        return ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message("One or more fields failed validation")
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage());
    }

    @ExceptionHandler(PaymentProcessorFactory.UnsupportedPaymentTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUnsupportedPayment(
            PaymentProcessorFactory.UnsupportedPaymentTypeException ex) {
        log.warn("Unsupported payment type: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PAYMENT_TYPE", ex.getMessage());
    }

    // ── 404 Not Found ─────────────────────────────────────────────────

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductNotFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleCustomerNotFound(CustomerNotFoundException ex) {
        log.warn("Customer not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", ex.getMessage());
    }

    // ── 500 Internal Server Error ─────────────────────────────────────

    /**
     * Catch-all for any unexpected exception that slips through.
     * Logs the full stack trace but returns a sanitised message to the caller.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.");
    }

    // ── Private helper ────────────────────────────────────────────────

    private ApiErrorResponse error(HttpStatus status, String code, String message) {
        return ApiErrorResponse.builder()
                .status(status.value())
                .error(code)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
