package com.quanlynhatro.tenant.api;

public record TenantSummaryResponse(
        Long userId,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        String contactAddress,
        boolean active) {}
