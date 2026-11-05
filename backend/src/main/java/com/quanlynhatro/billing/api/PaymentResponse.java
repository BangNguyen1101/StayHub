package com.quanlynhatro.billing.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String transactionCode,
        Long invoiceId,
        Long payerUserId,
        String providerCode,
        String paymentMethod,
        BigDecimal amount,
        String currencyCode,
        PaymentStatus status,
        String providerTransactionCode,
        LocalDateTime confirmedAt) {}
