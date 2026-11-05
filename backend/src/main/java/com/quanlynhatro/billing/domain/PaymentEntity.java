package com.quanlynhatro.billing.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.billing.api.PaymentStatus;

import jakarta.persistence.*;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "Payments", schema = "dbo")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentId", nullable = false)
    private Long id;

    @Column(name = "TransactionCode", nullable = false, length = 100, unique = true)
    private String transactionCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "InvoiceId", nullable = false)
    private InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PayerUserId", nullable = false)
    private AppUserEntity payerUser;

    @Column(name = "ProviderCode", nullable = false, length = 30)
    private String providerCode;

    @Column(name = "PaymentMethod", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "Amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "CurrencyCode", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "ProviderTransactionCode", length = 200)
    private String providerTransactionCode;

    @Column(name = "ExpiresAt")
    private LocalDateTime expiresAt;

    @Column(name = "ConfirmedAt")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConfirmedByUserId")
    private AppUserEntity confirmedByUser;

    @Column(name = "FailureReason", length = 1000)
    private String failureReason;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    protected PaymentEntity() {}

    public static PaymentEntity manual(
            String transactionCode,
            InvoiceEntity invoice,
            AppUserEntity payer,
            BigDecimal amount,
            String paymentMethod,
            AppUserEntity confirmer) {
        PaymentEntity p = new PaymentEntity();
        p.transactionCode = transactionCode;
        p.invoice = invoice;
        p.payerUser = payer;
        p.providerCode = "MANUAL";
        p.paymentMethod = paymentMethod;
        p.amount = amount;
        p.currencyCode = "VND";
        p.status = PaymentStatus.SUCCEEDED;
        p.confirmedAt = LocalDateTime.now();
        p.confirmedByUser = confirmer;
        return p;
    }

    @PreUpdate
    void updateAuditTimestamp() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public InvoiceEntity getInvoice() {
        return invoice;
    }

    public AppUserEntity getPayerUser() {
        return payerUser;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderTransactionCode() {
        return providerTransactionCode;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public AppUserEntity getConfirmedByUser() {
        return confirmedByUser;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
