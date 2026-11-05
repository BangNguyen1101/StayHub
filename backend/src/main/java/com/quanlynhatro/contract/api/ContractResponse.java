package com.quanlynhatro.contract.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractResponse(
        Long id,
        String contractCode,
        Long tenantUserId,
        Long roomId,
        String roomCode,
        Long rentalRequestId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate actualEndDate,
        BigDecimal monthlyRent,
        BigDecimal depositAmount,
        Integer billingCycleMonths,
        Short dueDay,
        ContractStatus status,
        String terms,
        String notes,
        LocalDateTime activatedAt,
        LocalDateTime terminatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
