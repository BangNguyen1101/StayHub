package com.quanlynhatro.utility.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServiceCreateRequest(
        @NotBlank @Size(max = 30) String serviceCode,
        @NotBlank @Size(max = 200) String serviceName,
        @NotNull FeePricingMode pricingMode,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2)
                BigDecimal defaultUnitPrice,
        @NotNull Boolean active) {}
