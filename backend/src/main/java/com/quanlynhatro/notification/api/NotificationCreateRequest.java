package com.quanlynhatro.notification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NotificationCreateRequest(
        @NotNull @Positive Long recipientUserId,
        @NotBlank @Size(max = 40) String notificationType,
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        @Size(max = 50) String relatedEntityType,
        @Positive Long relatedEntityId) {}
