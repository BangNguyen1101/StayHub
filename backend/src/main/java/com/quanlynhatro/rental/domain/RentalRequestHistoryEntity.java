package com.quanlynhatro.rental.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.rental.api.RentalRequestStatus;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "RentalRequestHistories", schema = "dbo")
public class RentalRequestHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RentalRequestHistoryId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RentalRequestId", nullable = false)
    private RentalRequestEntity rentalRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "OldStatus", length = 20)
    private RentalRequestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "NewStatus", nullable = false, length = 20)
    private RentalRequestStatus newStatus;

    @Column(name = "Note", length = 2000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ChangedByUserId", nullable = false)
    private AppUserEntity changedByUser;

    @Generated(event = EventType.INSERT)
    @Column(name = "ChangedAt", nullable = false)
    private LocalDateTime changedAt;

    protected RentalRequestHistoryEntity() {}

    public static RentalRequestHistoryEntity create(
            RentalRequestEntity rentalRequest,
            RentalRequestStatus oldStatus,
            RentalRequestStatus newStatus,
            String note,
            AppUserEntity changedByUser) {
        RentalRequestHistoryEntity history = new RentalRequestHistoryEntity();
        history.rentalRequest = rentalRequest;
        history.oldStatus = oldStatus;
        history.newStatus = newStatus;
        history.note = note;
        history.changedByUser = changedByUser;
        return history;
    }

    public Long getId() {
        return id;
    }

    public RentalRequestEntity getRentalRequest() {
        return rentalRequest;
    }

    public RentalRequestStatus getOldStatus() {
        return oldStatus;
    }

    public RentalRequestStatus getNewStatus() {
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
