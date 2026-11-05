package com.quanlynhatro.contract.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.contract.api.ContractDocumentStatus;
import com.quanlynhatro.contract.api.ContractDocumentType;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * One electronically signed document of a contract: the lease itself or a renewal appendix. The
 * content is frozen as a JSON snapshot when the document is issued, and the signed PDF is rendered
 * from that snapshot only, so later edits to the contract or the tenant profile cannot change what
 * was signed. Its SHA-256 is stored to detect tampering with the stored file.
 */
@Entity
@Table(name = "ContractDocuments", schema = "dbo")
public class ContractDocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ContractDocumentId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ContractId", nullable = false)
    private ContractEntity contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "DocumentType", nullable = false, length = 20)
    private ContractDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private ContractDocumentStatus status;

    @Column(name = "PreviousEndDate")
    private LocalDate previousEndDate;

    @Column(name = "NewEndDate")
    private LocalDate newEndDate;

    @Column(name = "RenewalMonths")
    private Integer renewalMonths;

    @Lob
    @Column(name = "ContentSnapshot", nullable = false, columnDefinition = "nvarchar(max)")
    private String contentSnapshot;

    @Column(name = "ContentHash", nullable = false, length = 64)
    private String contentHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SignerUserId", nullable = false)
    private AppUserEntity signerUser;

    @Column(name = "SignedAt")
    private LocalDateTime signedAt;

    @Column(name = "SignerIp", length = 64)
    private String signerIp;

    @Column(name = "SignerUserAgent", length = 500)
    private String signerUserAgent;

    @Column(name = "SignedPdfPath", length = 1000)
    private String signedPdfPath;

    @Column(name = "SignedPdfHash", length = 64)
    private String signedPdfHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CreatedByUserId", nullable = false)
    private AppUserEntity createdByUser;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "VoidedAt")
    private LocalDateTime voidedAt;

    protected ContractDocumentEntity() {}

    public static ContractDocumentEntity issueContract(
            ContractEntity contract,
            AppUserEntity signer,
            String contentSnapshot,
            String contentHash,
            AppUserEntity createdBy) {
        return create(
                contract,
                ContractDocumentType.CONTRACT,
                signer,
                contentSnapshot,
                contentHash,
                createdBy);
    }

    public static ContractDocumentEntity issueRenewal(
            ContractEntity contract,
            AppUserEntity signer,
            LocalDate previousEndDate,
            LocalDate newEndDate,
            int renewalMonths,
            String contentSnapshot,
            String contentHash) {
        ContractDocumentEntity document =
                create(
                        contract,
                        ContractDocumentType.RENEWAL,
                        signer,
                        contentSnapshot,
                        contentHash,
                        signer);
        document.previousEndDate = previousEndDate;
        document.newEndDate = newEndDate;
        document.renewalMonths = renewalMonths;
        return document;
    }

    private static ContractDocumentEntity create(
            ContractEntity contract,
            ContractDocumentType type,
            AppUserEntity signer,
            String contentSnapshot,
            String contentHash,
            AppUserEntity createdBy) {
        ContractDocumentEntity document = new ContractDocumentEntity();
        document.contract = contract;
        document.documentType = type;
        document.status = ContractDocumentStatus.AWAITING_SIGNATURE;
        document.signerUser = signer;
        document.contentSnapshot = contentSnapshot;
        document.contentHash = contentHash;
        document.createdByUser = createdBy;
        document.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        return document;
    }

    public void markSigned(
            LocalDateTime signedAt,
            String signerIp,
            String signerUserAgent,
            String signedPdfPath,
            String signedPdfHash) {
        this.status = ContractDocumentStatus.SIGNED;
        this.signedAt = signedAt;
        this.signerIp = signerIp;
        this.signerUserAgent = signerUserAgent;
        this.signedPdfPath = signedPdfPath;
        this.signedPdfHash = signedPdfHash;
    }

    public void markVoid() {
        this.status = ContractDocumentStatus.VOID;
        this.voidedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public ContractEntity getContract() {
        return contract;
    }

    public ContractDocumentType getDocumentType() {
        return documentType;
    }

    public ContractDocumentStatus getStatus() {
        return status;
    }

    public LocalDate getPreviousEndDate() {
        return previousEndDate;
    }

    public LocalDate getNewEndDate() {
        return newEndDate;
    }

    public Integer getRenewalMonths() {
        return renewalMonths;
    }

    public String getContentSnapshot() {
        return contentSnapshot;
    }

    public String getContentHash() {
        return contentHash;
    }

    public AppUserEntity getSignerUser() {
        return signerUser;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public String getSignerIp() {
        return signerIp;
    }

    public String getSignerUserAgent() {
        return signerUserAgent;
    }

    public String getSignedPdfPath() {
        return signedPdfPath;
    }

    public String getSignedPdfHash() {
        return signedPdfHash;
    }

    public AppUserEntity getCreatedByUser() {
        return createdByUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }
}
