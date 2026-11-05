package com.quanlynhatro.tenant.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantDetailResponse(
        Long userId,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        boolean active,
        String contactAddress,
        String identityDocumentType,
        String identityDocumentNumber,
        LocalDate identityIssuedDate,
        String identityIssuedPlace,
        String notes,
        LocalDateTime profileCreatedAt,
        LocalDateTime profileUpdatedAt) {}
