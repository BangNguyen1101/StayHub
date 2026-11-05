package com.quanlynhatro.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "SystemConfigs", schema = "dbo")
public class SystemConfigEntity {
    @Id
    @Column(name = "ConfigKey", length = 100)
    private String key;

    @Column(name = "ConfigValue", nullable = false, length = 1000)
    private String value;

    protected SystemConfigEntity() {}

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
