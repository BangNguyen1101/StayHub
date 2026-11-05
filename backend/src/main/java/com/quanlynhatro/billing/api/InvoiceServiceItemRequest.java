package com.quanlynhatro.billing.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InvoiceServiceItemRequest(
        @NotNull @Positive Long serviceId, @NotNull @DecimalMin("0.001") BigDecimal quantity) {}
