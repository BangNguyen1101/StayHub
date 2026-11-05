package com.quanlynhatro.tenant.api;

public record TenantProfileResponse(
        Long userId,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        String contactAddress,
        boolean active) {}
