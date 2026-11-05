package com.quanlynhatro.notification.api;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long recipientUserId,
        String notificationType,
        String title,
        String content,
        String relatedEntityType,
        Long relatedEntityId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt) {}
