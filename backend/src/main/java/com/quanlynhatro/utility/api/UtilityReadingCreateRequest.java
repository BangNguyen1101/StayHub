package com.quanlynhatro.utility.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UtilityReadingCreateRequest(
        @NotNull @Positive Long roomId,
        @NotNull UtilityType utilityType,
        @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String billingPeriod,
        @NotNull @DecimalMin("0.000") @Digits(integer = 15, fraction = 3) BigDecimal readingStart,
        @NotNull @DecimalMin("0.000") @Digits(integer = 15, fraction = 3) BigDecimal readingEnd,
        @NotNull LocalDate readingDate,
        @Size(max = 1000) String note) {}
