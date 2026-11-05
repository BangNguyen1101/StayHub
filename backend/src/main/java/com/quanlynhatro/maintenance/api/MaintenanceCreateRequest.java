package com.quanlynhatro.maintenance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaintenanceCreateRequest(
        @NotNull MaintenanceIssueType issueType,
        @NotBlank @Size(max = 300) String title,
        @NotBlank String description,
        @NotNull MaintenancePriority priority) {}
