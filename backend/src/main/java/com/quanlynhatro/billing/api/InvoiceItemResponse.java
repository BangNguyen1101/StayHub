package com.quanlynhatro.billing.api;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long id,
        InvoiceItemType itemType,
        String description,
        BigDecimal quantity,
        String unitName,
        BigDecimal unitPrice,
        BigDecimal amount,
        Long feeConfigId,
        Long utilityReadingId,
        Long serviceId) {}
