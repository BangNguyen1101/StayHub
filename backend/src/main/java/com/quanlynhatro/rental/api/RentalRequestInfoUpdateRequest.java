package com.quanlynhatro.rental.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RentalRequestInfoUpdateRequest(
        @NotBlank @Size(max = 200) String contactName,
        @NotBlank @Size(max = 30) String contactPhone,
        @Email @Size(max = 320) String contactEmail,
        @NotNull @Positive Integer expectedOccupantCount,
        @NotNull LocalDate expectedMoveInDate,
        @Size(max = 2000) String note) {}
