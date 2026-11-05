package com.quanlynhatro.billing.domain;

import jakarta.persistence.*;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "PaymentCallbacks", schema = "dbo")
public class PaymentCallbackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentCallbackId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PaymentId")
    private PaymentEntity payment;

    @Column(name = "ProviderCode", nullable = false, length = 30)
    private String providerCode;

    @Column(name = "ProviderTransactionCode", length = 200)
    private String providerTransactionCode;

    @Lob
    @Column(name = "PayloadJson", nullable = false, columnDefinition = "nvarchar(max)")
    private String payloadJson;

    @Column(name = "SignatureValid", nullable = false)
    private boolean signatureValid;

    @Column(name = "ProcessingStatus", nullable = false, length = 20)
    private String processingStatus;

    @Column(name = "ProcessingMessage", length = 1000)
    private String processingMessage;

    @Generated(event = EventType.INSERT)
    @Column(name = "ReceivedAt", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "ProcessedAt")
    private LocalDateTime processedAt;

    protected PaymentCallbackEntity() {}

    public Long getId() {
        return id;
    }

    public PaymentEntity getPayment() {
        return payment;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getProviderTransactionCode() {
        return providerTransactionCode;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public String getProcessingMessage() {
        return processingMessage;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
