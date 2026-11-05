package com.quanlynhatro.billing.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtResponse(
        Long invoiceId,
        String invoiceCode,
        Long contractId,
        Long roomId,
        String roomCode,
        Long payerUserId,
        String payerName,
        String billingPeriod,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        InvoiceStatus status,
        long overdueDays) {}
