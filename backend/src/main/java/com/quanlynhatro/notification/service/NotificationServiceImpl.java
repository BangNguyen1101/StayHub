package com.quanlynhatro.notification.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.notification.api.*;
import com.quanlynhatro.notification.domain.NotificationEntity;
import com.quanlynhatro.notification.repository.NotificationRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notifications;
    private final AppUserRepository users;

    public NotificationServiceImpl(NotificationRepository notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public NotificationResponse create(NotificationCreateRequest request, Long adminId) {
        requireRole(adminId, "ADMIN");
        AppUserEntity recipient =
                users.findByIdAndRole_Code(request.recipientUserId(), "TENANT")
                        .orElseThrow(NotificationNotFoundException::new);
        if (request.relatedEntityType() != null
                && request.relatedEntityId() != null
                && notifications
                        .existsByRecipientUser_IdAndNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
                                recipient.getId(),
                                request.notificationType(),
                                request.relatedEntityType(),
                                request.relatedEntityId())) {
            throw new NotificationConflictException("Notification already exists");
        }
        return toResponse(
                notifications.save(
                        NotificationEntity.create(
                                recipient,
                                request.notificationType(),
                                request.title(),
                                request.content(),
                                request.relatedEntityType(),
                                request.relatedEntityId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchForAdmin(Pageable pageable, Long adminId) {
        requireRole(adminId, "ADMIN");
        return notifications
                .findByRecipientUser_IdOrderByCreatedAtDesc(adminId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void notifyAdmins(
            String notificationType,
            String title,
            String content,
            String relatedEntityType,
            Long relatedEntityId) {
        for (AppUserEntity admin : users.findAllByRole_CodeAndActiveTrue("ADMIN")) {
            if (relatedEntityType != null
                    && relatedEntityId != null
                    && notifications
                            .existsByRecipientUser_IdAndNotificationTypeAndRelatedEntityTypeAndRelatedEntityId(
                                    admin.getId(),
                                    notificationType,
                                    relatedEntityType,
                                    relatedEntityId)) {
                continue;
            }
            notifications.save(
                    NotificationEntity.create(
                            admin,
                            notificationType,
                            title,
                            content,
                            relatedEntityType,
                            relatedEntityId));
        }
    }

    @Override
    @Transactional
    public NotificationResponse markReadForAdmin(Long id, Long adminId) {
        requireRole(adminId, "ADMIN");
        NotificationEntity notification =
                notifications
                        .findByIdAndRecipientUser_Id(id, adminId)
                        .orElseThrow(NotificationNotFoundException::new);
        notification.markRead(LocalDateTime.now(ZoneOffset.UTC));
        return toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public long adminUnreadCount(Long adminId) {
        requireRole(adminId, "ADMIN");
        return notifications.countByRecipientUser_IdAndReadFalse(adminId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchForTenant(
            NotificationQuery query, Long tenantId, Pageable pageable) {
        requireRole(tenantId, "TENANT");
        return notifications
                .searchForRecipient(tenantId, query.unread(), query.keyword(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getForTenant(Long id, Long tenantId) {
        requireRole(tenantId, "TENANT");
        return notifications
                .findByIdAndRecipientUser_Id(id, tenantId)
                .map(this::toResponse)
                .orElseThrow(NotificationNotFoundException::new);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long id, Long tenantId) {
        requireRole(tenantId, "TENANT");
        NotificationEntity notification =
                notifications
                        .findByIdAndRecipientUser_Id(id, tenantId)
                        .orElseThrow(NotificationNotFoundException::new);
        notification.markRead(LocalDateTime.now(ZoneOffset.UTC));
        return toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long tenantId) {
        requireRole(tenantId, "TENANT");
        return notifications.countByRecipientUser_IdAndReadFalse(tenantId);
    }

    private void requireRole(Long id, String role) {
        if (id == null || users.findByIdAndRole_Code(id, role).isEmpty()) {
            throw new NotificationAccessDeniedException();
        }
    }

    private NotificationResponse toResponse(NotificationEntity n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientUser().getId(),
                n.getNotificationType(),
                n.getTitle(),
                n.getContent(),
                n.getRelatedEntityType(),
                n.getRelatedEntityId(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt());
    }
}
