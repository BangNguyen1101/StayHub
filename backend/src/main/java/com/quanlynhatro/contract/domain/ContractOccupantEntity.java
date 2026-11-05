package com.quanlynhatro.contract.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;

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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ContractOccupants", schema = "dbo")
public class ContractOccupantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ContractOccupantId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ContractId", nullable = false)
    private ContractEntity contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId")
    private AppUserEntity user;

    @Column(name = "FullName", nullable = false, length = 200)
    private String fullName;

    @Column(name = "PhoneNumber", length = 30)
    private String phoneNumber;

    @Column(name = "IdentityDocumentNumber", length = 100)
    private String identityDocumentNumber;

    @Column(name = "IsPrimaryTenant", nullable = false)
    private boolean primaryTenant;

    @Column(name = "MoveInDate", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "MoveOutDate")
    private LocalDate moveOutDate;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    protected ContractOccupantEntity() {}

    public static ContractOccupantEntity create(
            ContractEntity contract,
            AppUserEntity user,
            String fullName,
            String phoneNumber,
            String identityDocumentNumber,
            boolean primaryTenant,
            LocalDate moveInDate) {
        ContractOccupantEntity occupant = new ContractOccupantEntity();
        occupant.contract = contract;
        occupant.user = user;
        occupant.fullName = fullName;
        occupant.phoneNumber = phoneNumber;
        occupant.identityDocumentNumber = identityDocumentNumber;
        occupant.primaryTenant = primaryTenant;
        occupant.moveInDate = moveInDate;
        return occupant;
    }

    public void update(
            AppUserEntity user,
            String fullName,
            String phoneNumber,
            String identityDocumentNumber,
            LocalDate moveInDate,
            LocalDate moveOutDate) {
        this.user = user;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.identityDocumentNumber = identityDocumentNumber;
        this.moveInDate = moveInDate;
        this.moveOutDate = moveOutDate;
    }

    public Long getId() {
        return id;
    }

    public ContractEntity getContract() {
        return contract;
    }

    public AppUserEntity getUser() {
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getIdentityDocumentNumber() {
        return identityDocumentNumber;
    }

    public boolean isPrimaryTenant() {
        return primaryTenant;
    }

    public LocalDate getMoveInDate() {
        return moveInDate;
    }

    public LocalDate getMoveOutDate() {
        return moveOutDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
