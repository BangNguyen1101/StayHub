package com.quanlynhatro.billing.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceCreateRequest(
        @NotNull @Positive Long contractId,
        @Positive Long payerUserId,
        @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String billingPeriod,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate,
        @Size(max = 30) String electricityFeeCode,
        @Size(max = 30) String waterFeeCode,
        @DecimalMin("0.000") BigDecimal electricityUsage,
        @DecimalMin("0.00") BigDecimal electricityAmount,
        @DecimalMin("0.000") BigDecimal waterUsage,
        @DecimalMin("0.00") BigDecimal waterAmount,
        @Valid List<InvoiceServiceItemRequest> services,
        @Size(max = 2000) String note) {
    public InvoiceCreateRequest(
            Long contractId,
            String billingPeriod,
            LocalDate issueDate,
            LocalDate dueDate,
            String electricityFeeCode,
            String waterFeeCode,
            List<InvoiceServiceItemRequest> services,
            String note) {
        this(
                contractId,
                null,
                billingPeriod,
                issueDate,
                dueDate,
                electricityFeeCode,
                waterFeeCode,
                null,
                null,
                null,
                null,
                services,
                note);
    }
}
