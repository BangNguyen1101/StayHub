package com.quanlynhatro.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminTenantCreateRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 200) String password,
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) String phoneNumber) {}
