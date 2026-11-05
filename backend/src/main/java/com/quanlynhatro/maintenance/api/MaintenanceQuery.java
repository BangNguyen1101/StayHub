package com.quanlynhatro.maintenance.api;

import java.time.LocalDate;

public record MaintenanceQuery(
        String keyword,
        MaintenanceStatus status,
        MaintenancePriority priority,
        MaintenanceIssueType issueType,
        Long roomId,
        Long reporterUserId,
        Long assignedToUserId,
        LocalDate createdFrom,
        LocalDate createdTo) {}
