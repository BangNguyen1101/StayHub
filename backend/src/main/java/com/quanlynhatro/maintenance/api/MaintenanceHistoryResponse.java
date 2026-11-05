package com.quanlynhatro.maintenance.api;

import java.time.LocalDateTime;

public record MaintenanceHistoryResponse(
        Long id,
        Long requestId,
        MaintenanceStatus oldStatus,
        MaintenanceStatus newStatus,
        String note,
        Long changedByUserId,
        LocalDateTime changedAt) {}
