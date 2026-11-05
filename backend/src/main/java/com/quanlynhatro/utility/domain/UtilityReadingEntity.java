package com.quanlynhatro.utility.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.room.domain.RoomEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "UtilityReadings", schema = "dbo")
public class UtilityReadingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UtilityReadingId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoomId", nullable = false)
    private RoomEntity room;

    @Column(name = "UtilityType", nullable = false, length = 20)
    private String utilityType;

    @Column(name = "BillingPeriod", nullable = false, columnDefinition = "char(7)")
    private String billingPeriod;

    @Column(name = "ReadingStart", nullable = false, precision = 18, scale = 3)
    private BigDecimal readingStart;

    @Column(name = "ReadingEnd", nullable = false, precision = 18, scale = 3)
    private BigDecimal readingEnd;

    @Column(name = "ReadingDate", nullable = false)
    private LocalDate readingDate;

    @Column(name = "IsLocked", nullable = false)
    private boolean locked;

    @Column(name = "Note", length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RecordedByUserId", nullable = false)
    private AppUserEntity recordedByUser;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    protected UtilityReadingEntity() {}

    public static UtilityReadingEntity create(
            RoomEntity room,
            String utilityType,
            String billingPeriod,
            BigDecimal readingStart,
            BigDecimal readingEnd,
            LocalDate readingDate,
            String note,
            AppUserEntity recordedByUser) {
        UtilityReadingEntity entity = new UtilityReadingEntity();
        entity.room = room;
        entity.utilityType = utilityType;
        entity.billingPeriod = billingPeriod;
        entity.readingStart = readingStart;
        entity.readingEnd = readingEnd;
        entity.readingDate = readingDate;
        entity.note = note;
        entity.recordedByUser = recordedByUser;
        entity.locked = false;
        return entity;
    }

    public void updateDetails(
            BigDecimal readingStart, BigDecimal readingEnd, LocalDate readingDate, String note) {
        this.readingStart = readingStart;
        this.readingEnd = readingEnd;
        this.readingDate = readingDate;
        this.note = note;
    }

    public void lock() {
        this.locked = true;
    }

    public Long getId() {
        return id;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public String getUtilityType() {
        return utilityType;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public BigDecimal getReadingStart() {
        return readingStart;
    }

    public BigDecimal getReadingEnd() {
        return readingEnd;
    }

    public BigDecimal getConsumption() {
        return readingEnd.subtract(readingStart);
    }

    public LocalDate getReadingDate() {
        return readingDate;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getNote() {
        return note;
    }

    public AppUserEntity getRecordedByUser() {
        return recordedByUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
