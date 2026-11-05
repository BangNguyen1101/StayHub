package com.quanlynhatro.utility.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeeConfigResponse(
        Long id,
        String feeCode,
        String feeName,
        String unitName,
        BigDecimal unitPrice,
        FeePricingMode pricingMode,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        LocalDateTime createdAt) {}
