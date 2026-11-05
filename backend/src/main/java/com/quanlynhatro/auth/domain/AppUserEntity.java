package com.quanlynhatro.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "AppUsers", schema = "dbo")
public class AppUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId", nullable = false)
    private Long id;

    @Column(name = "Username", nullable = false, length = 100)
    private String username;

    @Column(name = "Email", nullable = false, length = 320)
    private String email;

    @Column(name = "PasswordHash", nullable = false, length = 500)
    private String passwordHash;

    // Role is required while Spring Security reconstructs the authentication
    // from a JWT. Load it with the user so authorization never depends on an
    // open JPA session in the security filter.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "RoleId", nullable = false)
    private RoleEntity role;

    @Column(name = "FullName", nullable = false, length = 200)
    private String fullName;

    @Column(name = "PhoneNumber", length = 30)
    private String phoneNumber;

    @Column(name = "IsActive", nullable = false)
    private boolean active;

    @Column(name = "LastLoginAt")
    private LocalDateTime lastLoginAt;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    protected AppUserEntity() {}

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

    public static AppUserEntity createTenant(
            String username,
            String email,
            String passwordHash,
            RoleEntity role,
            String fullName,
            String phoneNumber) {
        AppUserEntity user = new AppUserEntity();
        user.username = username;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role;
        user.fullName = fullName;
        user.phoneNumber = phoneNumber;
        user.active = true;
        return user;
    }

    public void updateLastLogin(LocalDateTime value) {
        this.lastLoginAt = value;
    }

    public void deactivate() {
        this.active = false;
    }

    public void updateTenantProfile(String fullName, String phoneNumber) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public void updateAdminProfile(String email, String fullName, String phoneNumber) {
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public RoleEntity getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
