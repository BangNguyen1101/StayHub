package com.quanlynhatro.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Roles", schema = "dbo")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleId", nullable = false)
    private Byte id;

    @Column(name = "RoleCode", nullable = false, length = 20)
    private String code;

    @Column(name = "RoleName", nullable = false, length = 100)
    private String name;

    protected RoleEntity() {}

    public Byte getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
