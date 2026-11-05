package com.quanlynhatro.billing.domain;

import com.quanlynhatro.billing.api.InvoiceItemType;
import com.quanlynhatro.utility.domain.FeeConfigEntity;
import com.quanlynhatro.utility.domain.ServiceEntity;
import com.quanlynhatro.utility.domain.UtilityReadingEntity;

import jakarta.persistence.*;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "InvoiceItems", schema = "dbo")
public class InvoiceItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InvoiceItemId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "InvoiceId", nullable = false)
    private InvoiceEntity invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "ItemType", nullable = false, length = 30)
    private InvoiceItemType itemType;

    @Column(name = "Description", nullable = false, length = 500)
    private String description;

    @Column(name = "Quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "UnitName", nullable = false, length = 50)
    private String unitName;

    @Column(name = "UnitPrice", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "Amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FeeConfigId")
    private FeeConfigEntity feeConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UtilityReadingId")
    private UtilityReadingEntity utilityReading;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ServiceId")
    private ServiceEntity service;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    protected InvoiceItemEntity() {}

    public static InvoiceItemEntity create(
            InvoiceEntity invoice,
            InvoiceItemType type,
            String description,
            BigDecimal quantity,
            String unit,
            BigDecimal price,
            FeeConfigEntity fee,
            UtilityReadingEntity reading,
            ServiceEntity service) {
        InvoiceItemEntity e = new InvoiceItemEntity();
        e.invoice = invoice;
        e.itemType = type;
        e.description = description;
        e.quantity = quantity;
        e.unitName = unit;
        e.unitPrice = price;
        e.amount = price.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        e.feeConfig = fee;
        e.utilityReading = reading;
        e.service = service;
        return e;
    }

    public Long getId() {
        return id;
    }

    public InvoiceEntity getInvoice() {
        return invoice;
    }

    public InvoiceItemType getItemType() {
        return itemType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnitName() {
        return unitName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public FeeConfigEntity getFeeConfig() {
        return feeConfig;
    }

    public UtilityReadingEntity getUtilityReading() {
        return utilityReading;
    }

    public ServiceEntity getService() {
        return service;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
