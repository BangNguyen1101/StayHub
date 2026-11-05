package com.quanlynhatro.contract.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractSummaryResponse(
        Long id,
        String contractCode,
        Long tenantUserId,
        String tenantName,
        Long roomId,
        String roomCode,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        ContractStatus status) {}
