package com.quanlynhatro.rental.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.rental.api.RentalRequestStatus;
import com.quanlynhatro.room.domain.RoomEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "RentalRequests", schema = "dbo")
public class RentalRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RentalRequestId", nullable = false)
    private Long id;

    @Column(name = "RequestCode", nullable = false, length = 50, unique = true)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ApplicantUserId", nullable = false)
    private AppUserEntity applicantUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoomId", nullable = false)
    private RoomEntity room;

    @Column(name = "ContactName", nullable = false, length = 200)
    private String contactName;

    @Column(name = "ContactPhone", nullable = false, length = 30)
    private String contactPhone;

    @Column(name = "ContactEmail", length = 320)
    private String contactEmail;

    @Column(name = "ExpectedOccupantCount", nullable = false)
    private Integer expectedOccupantCount;

    @Column(name = "ExpectedMoveInDate", nullable = false)
    private LocalDate expectedMoveInDate;

    @Column(name = "Note", length = 2000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private RentalRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProcessedByUserId")
    private AppUserEntity processedByUser;

    @Column(name = "ProcessedAt")
    private LocalDateTime processedAt;

    @Column(name = "ProcessingNote", length = 2000)
    private String processingNote;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    protected RentalRequestEntity() {}

    public static RentalRequestEntity create(
            String requestCode,
            AppUserEntity applicantUser,
            RoomEntity room,
            String contactName,
            String contactPhone,
            String contactEmail,
            Integer expectedOccupantCount,
            LocalDate expectedMoveInDate,
            String note) {
        RentalRequestEntity request = new RentalRequestEntity();
        request.requestCode = requestCode;
        request.applicantUser = applicantUser;
        request.room = room;
        request.contactName = contactName;
        request.contactPhone = contactPhone;
        request.contactEmail = contactEmail;
        request.expectedOccupantCount = expectedOccupantCount;
        request.expectedMoveInDate = expectedMoveInDate;
        request.note = note;
        request.status = RentalRequestStatus.PENDING;
        return request;
    }

    public void updateInformation(
            String contactName,
            String contactPhone,
            String contactEmail,
            Integer expectedOccupantCount,
            LocalDate expectedMoveInDate,
            String note) {
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.expectedOccupantCount = expectedOccupantCount;
        this.expectedMoveInDate = expectedMoveInDate;
        this.note = note;
    }

    public void changeStatus(
            RentalRequestStatus newStatus,
            AppUserEntity processedByUser,
            LocalDateTime processedAt,
            String processingNote) {
        this.status = newStatus;
        this.processedByUser = processedByUser;
        this.processedAt = processedAt;
        this.processingNote = processingNote;
    }

    public Long getId() {
        return id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public AppUserEntity getApplicantUser() {
        return applicantUser;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public Integer getExpectedOccupantCount() {
        return expectedOccupantCount;
    }

    public LocalDate getExpectedMoveInDate() {
        return expectedMoveInDate;
    }

    public String getNote() {
        return note;
    }

    public RentalRequestStatus getStatus() {
        return status;
    }

    public AppUserEntity getProcessedByUser() {
        return processedByUser;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public String getProcessingNote() {
        return processingNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
