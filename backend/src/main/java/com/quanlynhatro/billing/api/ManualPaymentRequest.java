package com.quanlynhatro.billing.api;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ManualPaymentRequest(
        @NotBlank @Size(max = 100) String transactionCode,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(max = 30) String paymentMethod) {}
