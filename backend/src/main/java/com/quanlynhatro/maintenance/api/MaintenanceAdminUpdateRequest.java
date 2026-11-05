package com.quanlynhatro.maintenance.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MaintenanceAdminUpdateRequest(
        @NotNull MaintenanceStatus status,
        @Positive Long assignedToUserId,
        @DecimalMin("0.00") BigDecimal estimatedCost,
        @DecimalMin("0.00") BigDecimal actualCost,
        @Size(max = 2000) String resolutionNote,
        @Size(max = 2000) String rejectionReason,
        @Size(max = 2000) String note) {}
