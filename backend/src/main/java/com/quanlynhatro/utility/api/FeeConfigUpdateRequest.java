package com.quanlynhatro.utility.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeConfigUpdateRequest(
        @NotBlank @Size(max = 200) String feeName,
        @NotBlank @Size(max = 50) String unitName,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal unitPrice,
        @NotNull FeePricingMode pricingMode,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @NotNull Boolean active) {}
