package com.quanlynhatro.maintenance.api;

import java.time.LocalDateTime;

public record MaintenanceAttachmentResponse(
        Long id,
        String originalFileName,
        String contentType,
        long fileSizeBytes,
        LocalDateTime createdAt) {}
