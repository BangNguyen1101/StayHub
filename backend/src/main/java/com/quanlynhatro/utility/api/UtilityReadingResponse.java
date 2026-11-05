package com.quanlynhatro.utility.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UtilityReadingResponse(
        Long id,
        Long roomId,
        UtilityType utilityType,
        String billingPeriod,
        BigDecimal readingStart,
        BigDecimal readingEnd,
        BigDecimal consumption,
        LocalDate readingDate,
        boolean locked,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
