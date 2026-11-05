package com.quanlynhatro.notification.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;

import jakarta.persistence.*;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notifications", schema = "dbo")
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RecipientUserId", nullable = false)
    private AppUserEntity recipientUser;

    @Column(name = "NotificationType", nullable = false, length = 40)
    private String notificationType;

    @Column(name = "Title", nullable = false, length = 300)
    private String title;

    @Column(name = "Content", nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(name = "RelatedEntityType", length = 50)
    private String relatedEntityType;

    @Column(name = "RelatedEntityId")
    private Long relatedEntityId;

    @Column(name = "IsRead", nullable = false)
    private boolean read;

    @Column(name = "ReadAt")
    private LocalDateTime readAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    protected NotificationEntity() {}

    public static NotificationEntity create(
            AppUserEntity recipient,
            String type,
            String title,
            String content,
            String relatedType,
            Long relatedId) {
        NotificationEntity entity = new NotificationEntity();
        entity.recipientUser = recipient;
        entity.notificationType = type;
        entity.title = title;
        entity.content = content;
        entity.relatedEntityType = relatedType;
        entity.relatedEntityId = relatedId;
        entity.read = false;
        return entity;
    }

    public void markRead(LocalDateTime at) {
        if (!read) {
            read = true;
            readAt = at;
        }
    }

    public Long getId() {
        return id;
    }

    public AppUserEntity getRecipientUser() {
        return recipientUser;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
