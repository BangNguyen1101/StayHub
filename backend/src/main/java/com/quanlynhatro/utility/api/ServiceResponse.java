package com.quanlynhatro.utility.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(
        Long id,
        String serviceCode,
        String serviceName,
        FeePricingMode pricingMode,
        BigDecimal defaultUnitPrice,
        boolean active,
        LocalDateTime createdAt) {}
