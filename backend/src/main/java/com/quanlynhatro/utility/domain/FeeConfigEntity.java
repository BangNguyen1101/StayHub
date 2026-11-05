package com.quanlynhatro.utility.domain;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "FeeConfigs", schema = "dbo")
public class FeeConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeeConfigId", nullable = false)
    private Long id;

    @Column(name = "FeeCode", nullable = false, length = 30)
    private String feeCode;

    @Column(name = "FeeName", nullable = false, length = 200)
    private String feeName;

    @Column(name = "UnitName", nullable = false, length = 50)
    private String unitName;

    @Column(name = "UnitPrice", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "PricingMode", nullable = false, length = 20)
    private String pricingMode;

    @Column(name = "EffectiveFrom", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "EffectiveTo")
    private LocalDate effectiveTo;

    @Column(name = "IsActive", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CreatedByUserId", nullable = false)
    private AppUserEntity createdByUser;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    protected FeeConfigEntity() {}

    public static FeeConfigEntity create(
            String feeCode,
            String feeName,
            String unitName,
            BigDecimal unitPrice,
            String pricingMode,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean active,
            AppUserEntity createdByUser) {
        FeeConfigEntity entity = new FeeConfigEntity();
        entity.feeCode = feeCode;
        entity.feeName = feeName;
        entity.unitName = unitName;
        entity.unitPrice = unitPrice;
        entity.pricingMode = pricingMode;
        entity.effectiveFrom = effectiveFrom;
        entity.effectiveTo = effectiveTo;
        entity.active = active;
        entity.createdByUser = createdByUser;
        return entity;
    }

    public void updateDetails(
            String feeName,
            String unitName,
            BigDecimal unitPrice,
            String pricingMode,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean active) {
        this.feeName = feeName;
        this.unitName = unitName;
        this.unitPrice = unitPrice;
        this.pricingMode = pricingMode;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getFeeCode() {
        return feeCode;
    }

    public String getFeeName() {
        return feeName;
    }

    public String getUnitName() {
        return unitName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getPricingMode() {
        return pricingMode;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public boolean isActive() {
        return active;
    }

    public AppUserEntity getCreatedByUser() {
        return createdByUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
