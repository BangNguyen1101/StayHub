package com.quanlynhatro.notification.service;

import com.quanlynhatro.notification.api.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    NotificationResponse create(NotificationCreateRequest request, Long adminId);

    Page<NotificationResponse> searchForAdmin(Pageable pageable, Long adminId);

    void notifyAdmins(
            String notificationType,
            String title,
            String content,
            String relatedEntityType,
            Long relatedEntityId);

    NotificationResponse markReadForAdmin(Long id, Long adminId);

    long adminUnreadCount(Long adminId);

    Page<NotificationResponse> searchForTenant(
            NotificationQuery query, Long tenantId, Pageable pageable);

    NotificationResponse getForTenant(Long id, Long tenantId);

    NotificationResponse markRead(Long id, Long tenantId);

    long unreadCount(Long tenantId);
}
