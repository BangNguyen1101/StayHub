package com.quanlynhatro.utility.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Services", schema = "dbo")
public class ServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ServiceId", nullable = false)
    private Long id;

    @Column(name = "ServiceCode", nullable = false, length = 30, unique = true)
    private String serviceCode;

    @Column(name = "ServiceName", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "PricingMode", nullable = false, length = 20)
    private String pricingMode;

    @Column(name = "DefaultUnitPrice", nullable = false, precision = 19, scale = 2)
    private BigDecimal defaultUnitPrice;

    @Column(name = "IsActive", nullable = false)
    private boolean active;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    protected ServiceEntity() {}

    public static ServiceEntity create(
            String serviceCode,
            String serviceName,
            String pricingMode,
            BigDecimal defaultUnitPrice,
            boolean active) {
        ServiceEntity entity = new ServiceEntity();
        entity.serviceCode = serviceCode;
        entity.serviceName = serviceName;
        entity.pricingMode = pricingMode;
        entity.defaultUnitPrice = defaultUnitPrice;
        entity.active = active;
        return entity;
    }

    public void updateDetails(
            String serviceName, String pricingMode, BigDecimal defaultUnitPrice, boolean active) {
        this.serviceName = serviceName;
        this.pricingMode = pricingMode;
        this.defaultUnitPrice = defaultUnitPrice;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPricingMode() {
        return pricingMode;
    }

    public BigDecimal getDefaultUnitPrice() {
        return defaultUnitPrice;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
