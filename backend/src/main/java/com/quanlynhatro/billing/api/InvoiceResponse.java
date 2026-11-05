package com.quanlynhatro.billing.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceCode,
        Long contractId,
        Long roomId,
        String roomCode,
        Long payerUserId,
        String billingPeriod,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal rentAmount,
        BigDecimal utilityAmount,
        BigDecimal serviceAmount,
        BigDecimal adjustmentAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        InvoiceStatus status,
        String note,
        LocalDateTime issuedAt,
        List<InvoiceItemResponse> items) {}
