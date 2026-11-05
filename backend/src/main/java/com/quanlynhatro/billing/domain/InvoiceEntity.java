package com.quanlynhatro.billing.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.billing.api.InvoiceStatus;
import com.quanlynhatro.contract.domain.ContractEntity;
import com.quanlynhatro.room.domain.RoomEntity;

import jakarta.persistence.*;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "Invoices", schema = "dbo")
public class InvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InvoiceId", nullable = false)
    private Long id;

    @Column(name = "InvoiceCode", nullable = false, length = 50, unique = true)
    private String invoiceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ContractId", nullable = false)
    private ContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoomId", nullable = false)
    private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PayerUserId", nullable = false)
    private AppUserEntity payerUser;

    @Column(name = "BillingPeriod", nullable = false, columnDefinition = "char(7)")
    private String billingPeriod;

    @Column(name = "IssueDate", nullable = false)
    private LocalDate issueDate;

    @Column(name = "DueDate", nullable = false)
    private LocalDate dueDate;

    @Column(name = "RentAmount", nullable = false, precision = 19, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "UtilityAmount", nullable = false, precision = 19, scale = 2)
    private BigDecimal utilityAmount;

    @Column(name = "ServiceAmount", nullable = false, precision = 19, scale = 2)
    private BigDecimal serviceAmount;

    @Column(name = "AdjustmentAmount", nullable = false, precision = 19, scale = 2)
    private BigDecimal adjustmentAmount;

    @Column(name = "TotalAmount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "PaidAmount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "Note", length = 2000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CreatedByUserId", nullable = false)
    private AppUserEntity createdByUser;

    @Column(name = "IssuedAt")
    private LocalDateTime issuedAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    protected InvoiceEntity() {}

    public static InvoiceEntity draft(
            String code,
            ContractEntity contract,
            RoomEntity room,
            AppUserEntity payer,
            String period,
            LocalDate issue,
            LocalDate due,
            String note,
            AppUserEntity creator) {
        InvoiceEntity e = new InvoiceEntity();
        e.invoiceCode = code;
        e.contract = contract;
        e.room = room;
        e.payerUser = payer;
        e.billingPeriod = period;
        e.issueDate = issue;
        e.dueDate = due;
        e.note = note;
        e.createdByUser = creator;
        e.rentAmount = BigDecimal.ZERO;
        e.utilityAmount = BigDecimal.ZERO;
        e.serviceAmount = BigDecimal.ZERO;
        e.adjustmentAmount = BigDecimal.ZERO;
        e.totalAmount = BigDecimal.ZERO;
        e.paidAmount = BigDecimal.ZERO;
        e.status = InvoiceStatus.DRAFT;
        return e;
    }

    public void setAmounts(
            BigDecimal rent, BigDecimal utility, BigDecimal service, BigDecimal adjustment) {
        rentAmount = rent;
        utilityAmount = utility;
        serviceAmount = service;
        adjustmentAmount = adjustment;
        totalAmount = rent.add(utility).add(service).add(adjustment);
    }

    @PreUpdate
    void updateAuditTimestamp() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void issue(LocalDateTime at) {
        if (status != InvoiceStatus.DRAFT)
            throw new IllegalStateException("Only draft invoices can be issued");
        status = InvoiceStatus.UNPAID;
        issuedAt = at;
    }

    public void applyPayment(BigDecimal paid, LocalDate today) {
        if (status == InvoiceStatus.DRAFT || status == InvoiceStatus.CANCELLED)
            throw new IllegalStateException("Invoice cannot receive payment");
        if (paid.compareTo(totalAmount) > 0)
            throw new IllegalArgumentException("Paid amount exceeds invoice total");
        paidAmount = paid;
        status =
                paid.compareTo(totalAmount) == 0
                        ? InvoiceStatus.PAID
                        : (dueDate.isBefore(today)
                                ? InvoiceStatus.OVERDUE
                                : (paid.signum() > 0
                                        ? InvoiceStatus.PARTIALLY_PAID
                                        : InvoiceStatus.UNPAID));
    }

    public void cancel() {
        if (status == InvoiceStatus.PAID || status == InvoiceStatus.CANCELLED)
            throw new IllegalStateException("Invoice cannot be cancelled");
        status = InvoiceStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public ContractEntity getContract() {
        return contract;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public AppUserEntity getPayerUser() {
        return payerUser;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getRentAmount() {
        return rentAmount;
    }

    public BigDecimal getUtilityAmount() {
        return utilityAmount;
    }

    public BigDecimal getServiceAmount() {
        return serviceAmount;
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public AppUserEntity getCreatedByUser() {
        return createdByUser;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
