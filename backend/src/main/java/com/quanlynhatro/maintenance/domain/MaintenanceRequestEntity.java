package com.quanlynhatro.maintenance.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.maintenance.api.*;
import com.quanlynhatro.room.domain.RoomEntity;

import jakarta.persistence.*;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MaintenanceRequests", schema = "dbo")
public class MaintenanceRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaintenanceRequestId", nullable = false)
    private Long id;

    @Column(name = "RequestCode", nullable = false, length = 50, unique = true)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReporterUserId", nullable = false)
    private AppUserEntity reporterUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoomId", nullable = false)
    private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ContractId")
    private ContractEntity contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "IssueType", nullable = false, length = 30)
    private MaintenanceIssueType issueType;

    @Column(name = "Title", nullable = false, length = 300)
    private String title;

    @Lob
    @Column(name = "Description", nullable = false, columnDefinition = "nvarchar(max)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "Priority", nullable = false, length = 20)
    private MaintenancePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private MaintenanceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AssignedToUserId")
    private AppUserEntity assignedToUser;

    @Column(name = "EstimatedCost", precision = 19, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "ActualCost", precision = 19, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "ResolutionNote", length = 2000)
    private String resolutionNote;

    @Column(name = "RejectionReason", length = 2000)
    private String rejectionReason;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ResolvedAt")
    private LocalDateTime resolvedAt;

    protected MaintenanceRequestEntity() {}

    public static MaintenanceRequestEntity create(
            String code,
            AppUserEntity reporter,
            RoomEntity room,
            ContractEntity contract,
            MaintenanceCreateRequest request) {
        MaintenanceRequestEntity entity = new MaintenanceRequestEntity();
        entity.requestCode = code;
        entity.reporterUser = reporter;
        entity.room = room;
        entity.contract = contract;
        entity.issueType = request.issueType();
        entity.title = request.title();
        entity.description = request.description();
        entity.priority = request.priority();
        entity.status = MaintenanceStatus.OPEN;
        return entity;
    }

    public void applyAdminUpdate(
            MaintenanceAdminUpdateRequest request, AppUserEntity assignee, LocalDateTime now) {
        this.status = request.status();
        this.assignedToUser = assignee;
        this.estimatedCost = request.estimatedCost();
        this.actualCost = request.actualCost();
        this.resolutionNote = request.resolutionNote();
        this.rejectionReason = request.rejectionReason();
        if (request.status() == MaintenanceStatus.RESOLVED
                || request.status() == MaintenanceStatus.CLOSED) {
            this.resolvedAt = this.resolvedAt == null ? now : this.resolvedAt;
        }
    }

    public Long getId() {
        return id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public AppUserEntity getReporterUser() {
        return reporterUser;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public ContractEntity getContract() {
        return contract;
    }

    public MaintenanceIssueType getIssueType() {
        return issueType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public MaintenancePriority getPriority() {
        return priority;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public AppUserEntity getAssignedToUser() {
        return assignedToUser;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
