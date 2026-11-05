package com.quanlynhatro.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantSelfProfileUpdateRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) String phoneNumber,
        @Size(max = 500) String contactAddress) {}
