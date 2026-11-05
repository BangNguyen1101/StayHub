package com.quanlynhatro.maintenance.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.maintenance.api.MaintenanceStatus;

import jakarta.persistence.*;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "MaintenanceRequestHistories", schema = "dbo")
public class MaintenanceRequestHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaintenanceRequestHistoryId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaintenanceRequestId", nullable = false)
    private MaintenanceRequestEntity request;

    @Enumerated(EnumType.STRING)
    @Column(name = "OldStatus", length = 20)
    private MaintenanceStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "NewStatus", nullable = false, length = 20)
    private MaintenanceStatus newStatus;

    @Column(name = "Note", length = 2000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ChangedByUserId", nullable = false)
    private AppUserEntity changedByUser;

    @Generated(event = EventType.INSERT)
    @Column(name = "ChangedAt", nullable = false)
    private LocalDateTime changedAt;

    protected MaintenanceRequestHistoryEntity() {}

    public static MaintenanceRequestHistoryEntity create(
            MaintenanceRequestEntity request,
            MaintenanceStatus oldStatus,
            MaintenanceStatus newStatus,
            String note,
            AppUserEntity actor) {
        MaintenanceRequestHistoryEntity h = new MaintenanceRequestHistoryEntity();
        h.request = request;
        h.oldStatus = oldStatus;
        h.newStatus = newStatus;
        h.note = note;
        h.changedByUser = actor;
        return h;
    }

    public Long getId() {
        return id;
    }

    public MaintenanceRequestEntity getRequest() {
        return request;
    }

    public MaintenanceStatus getOldStatus() {
        return oldStatus;
    }

    public MaintenanceStatus getNewStatus() {
        return newStatus;
    }

    public String getNote() {
        return note;
    }

    public AppUserEntity getChangedByUser() {
        return changedByUser;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
