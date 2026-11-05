package com.quanlynhatro.contract.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ContractOccupantUpdateRequest(
        @Positive Long userId,
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) String phoneNumber,
        @Size(max = 100) String identityDocumentNumber,
        @NotNull LocalDate moveInDate,
        LocalDate moveOutDate) {}
