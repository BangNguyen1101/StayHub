package com.quanlynhatro.maintenance.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MaintenanceResponse(
        Long id,
        String requestCode,
        Long reporterUserId,
        Long roomId,
        String roomCode,
        Long contractId,
        MaintenanceIssueType issueType,
        String title,
        String description,
        MaintenancePriority priority,
        MaintenanceStatus status,
        Long assignedToUserId,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        String resolutionNote,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        String reporterName,
        String assignedToName,
        List<MaintenanceAttachmentResponse> attachments) {}
