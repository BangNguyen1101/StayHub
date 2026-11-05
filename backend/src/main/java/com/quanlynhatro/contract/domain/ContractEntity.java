package com.quanlynhatro.contract.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.contract.api.ContractStatus;
import com.quanlynhatro.rental.domain.RentalRequestEntity;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Contracts", schema = "dbo")
public class ContractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ContractId", nullable = false)
    private Long id;

    @Column(name = "ContractCode", nullable = false, length = 50, unique = true)
    private String contractCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TenantUserId", nullable = false)
    private AppUserEntity tenantUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoomId", nullable = false)
    private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RentalRequestId")
    private RentalRequestEntity rentalRequest;

    @Column(name = "StartDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "EndDate", nullable = false)
    private LocalDate endDate;

    @Column(name = "ActualEndDate")
    private LocalDate actualEndDate;

    @Column(name = "MonthlyRent", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "DepositAmount", nullable = false, precision = 19, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "BillingCycleMonths", nullable = false)
    private Integer billingCycleMonths;

    @Column(name = "DueDay", nullable = false)
    private Short dueDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private ContractStatus status;

    @Lob
    @Column(name = "Terms", columnDefinition = "nvarchar(max)")
    private String terms;

    @Column(name = "Notes", length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CreatedByUserId", nullable = false)
    private AppUserEntity createdByUser;

    @Column(name = "ActivatedAt")
    private LocalDateTime activatedAt;

    @Column(name = "TerminatedAt")
    private LocalDateTime terminatedAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    protected ContractEntity() {}

    public static ContractEntity create(
            String contractCode,
            AppUserEntity tenantUser,
            RoomEntity room,
            RentalRequestEntity rentalRequest,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal monthlyRent,
            BigDecimal depositAmount,
            Integer billingCycleMonths,
            Short dueDay,
            String terms,
            String notes,
            AppUserEntity createdByUser) {
        ContractEntity contract = new ContractEntity();
        contract.contractCode = contractCode;
        contract.tenantUser = tenantUser;
        contract.room = room;
        contract.rentalRequest = rentalRequest;
        contract.startDate = startDate;
        contract.endDate = endDate;
        contract.monthlyRent = monthlyRent;
        contract.depositAmount = depositAmount;
        contract.billingCycleMonths = billingCycleMonths;
        contract.dueDay = dueDay;
        contract.terms = terms;
        contract.notes = notes;
        contract.createdByUser = createdByUser;
        contract.status = ContractStatus.DRAFT;
        return contract;
    }

    public void updateDetails(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal monthlyRent,
            BigDecimal depositAmount,
            Integer billingCycleMonths,
            Short dueDay,
            String terms,
            String notes) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.depositAmount = depositAmount;
        this.billingCycleMonths = billingCycleMonths;
        this.dueDay = dueDay;
        this.terms = terms;
        this.notes = notes;
    }

    public void changeStatus(ContractStatus status) {
        this.status = status;
    }

    public void renewUntil(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void markActivated(LocalDateTime value) {
        this.status = ContractStatus.ACTIVE;
        this.activatedAt = value;
    }

    public void markTerminated(LocalDate actualEndDate, LocalDateTime value) {
        this.actualEndDate = actualEndDate;
        this.terminatedAt = value;
        this.status = ContractStatus.TERMINATED;
    }

    public void markExpired(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
        this.status = ContractStatus.EXPIRED;
    }

    public Long getId() {
        return id;
    }

    public String getContractCode() {
        return contractCode;
    }

    public AppUserEntity getTenantUser() {
        return tenantUser;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public RentalRequestEntity getRentalRequest() {
        return rentalRequest;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public Integer getBillingCycleMonths() {
        return billingCycleMonths;
    }

    public Short getDueDay() {
        return dueDay;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public String getTerms() {
        return terms;
    }

    public String getNotes() {
        return notes;
    }

    public AppUserEntity getCreatedByUser() {
        return createdByUser;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public LocalDateTime getTerminatedAt() {
        return terminatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
