package com.quanlynhatro.billing.api;

import java.math.BigDecimal;

public record PaymentQrResponse(
        Long invoiceId,
        String invoiceCode,
        BigDecimal amount,
        String currencyCode,
        String bankCode,
        String accountNumber,
        String accountName,
        String transferContent,
        String qrUrl) {}
