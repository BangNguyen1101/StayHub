package com.quanlynhatro.contract.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractOccupantResponse(
        Long id,
        Long contractId,
        Long userId,
        String fullName,
        String phoneNumber,
        String identityDocumentNumber,
        boolean primaryTenant,
        LocalDate moveInDate,
        LocalDate moveOutDate,
        LocalDateTime createdAt) {}
