package com.quanlynhatro.contract.domain;

import com.quanlynhatro.auth.domain.AppUserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "ContractHistories", schema = "dbo")
public class ContractHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ContractHistoryId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ContractId", nullable = false)
    private ContractEntity contract;

    @Column(name = "ActionCode", nullable = false, length = 30)
    private String actionCode;

    @Lob
    @Column(name = "BeforeData", columnDefinition = "nvarchar(max)")
    private String beforeData;

    @Lob
    @Column(name = "AfterData", columnDefinition = "nvarchar(max)")
    private String afterData;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PerformedByUserId", nullable = false)
    private AppUserEntity performedByUser;

    @Generated(event = EventType.INSERT)
    @Column(name = "PerformedAt", nullable = false)
    private LocalDateTime performedAt;

    protected ContractHistoryEntity() {}

    public static ContractHistoryEntity create(
            ContractEntity contract,
            String actionCode,
            String beforeData,
            String afterData,
            AppUserEntity performedByUser) {
        ContractHistoryEntity history = new ContractHistoryEntity();
        history.contract = contract;
        history.actionCode = actionCode;
        history.beforeData = beforeData;
        history.afterData = afterData;
        history.performedByUser = performedByUser;
        return history;
    }

    public Long getId() {
        return id;
    }

    public ContractEntity getContract() {
        return contract;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public AppUserEntity getPerformedByUser() {
        return performedByUser;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }
}
