package com.quanlynhatro.tenant.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AdminTenantProfileUpdateRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) String phoneNumber,
        @Size(max = 500) String contactAddress,
        @Size(max = 50) String identityDocumentType,
        @Size(max = 100) String identityDocumentNumber,
        LocalDate identityIssuedDate,
        @Size(max = 200) String identityIssuedPlace,
        @Size(max = 1000) String notes) {}
