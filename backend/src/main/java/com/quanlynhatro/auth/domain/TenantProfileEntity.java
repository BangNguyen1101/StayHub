package com.quanlynhatro.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "TenantProfiles", schema = "dbo")
public class TenantProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TenantProfileId", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false, unique = true)
    private AppUserEntity user;

    @Column(name = "ContactAddress", length = 500)
    private String contactAddress;

    @Column(name = "IdentityDocumentType", length = 50)
    private String identityDocumentType;

    @Column(name = "IdentityDocumentNumber", length = 100)
    private String identityDocumentNumber;

    @Column(name = "IdentityIssuedDate")
    private LocalDate identityIssuedDate;

    @Column(name = "IdentityIssuedPlace", length = 200)
    private String identityIssuedPlace;

    @Column(name = "Notes", length = 1000)
    private String notes;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    protected TenantProfileEntity() {}

    @PrePersist
    void initializeAuditTimestamps() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void updateAuditTimestamp() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public static TenantProfileEntity createFor(AppUserEntity user) {
        TenantProfileEntity profile = new TenantProfileEntity();
        profile.user = user;
        return profile;
    }

    public void updateContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public void updateAdminFields(
            String contactAddress,
            String identityDocumentType,
            String identityDocumentNumber,
            LocalDate identityIssuedDate,
            String identityIssuedPlace,
            String notes) {
        this.contactAddress = contactAddress;
        this.identityDocumentType = identityDocumentType;
        this.identityDocumentNumber = identityDocumentNumber;
        this.identityIssuedDate = identityIssuedDate;
        this.identityIssuedPlace = identityIssuedPlace;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public AppUserEntity getUser() {
        return user;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public String getIdentityDocumentType() {
        return identityDocumentType;
    }

    public String getIdentityDocumentNumber() {
        return identityDocumentNumber;
    }

    public LocalDate getIdentityIssuedDate() {
        return identityIssuedDate;
    }

    public String getIdentityIssuedPlace() {
        return identityIssuedPlace;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
