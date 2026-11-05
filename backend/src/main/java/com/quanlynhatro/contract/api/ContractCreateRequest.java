package com.quanlynhatro.contract.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractCreateRequest(
        @NotBlank @Size(max = 50) String contractCode,
        @NotNull @Positive Long tenantUserId,
        @NotNull @Positive Long roomId,
        @Positive Long rentalRequestId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyRent,
        @NotNull @DecimalMin("0.00") BigDecimal depositAmount,
        @NotNull @Min(1) Integer billingCycleMonths,
        @NotNull @Min(1) @Max(28) Short dueDay,
        @Size(max = 2000) String notes,
        String terms) {}
