package com.quanlynhatro.contract.api;

import java.time.LocalDateTime;

public record ContractHistoryResponse(
        Long id,
        Long contractId,
        String actionCode,
        String beforeData,
        String afterData,
        Long performedByUserId,
        LocalDateTime performedAt) {}
