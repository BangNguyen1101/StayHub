package com.quanlynhatro.auth.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class AuditTimestampTest {
    @Test
    void appUserInitializesAndUpdatesAuditTimestamps() {
        AppUserEntity user = new AppUserEntity();

        user.initializeAuditTimestamps();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        LocalDateTime previousUpdatedAt = user.getUpdatedAt();

        user.updateAuditTimestamp();

        assertTrue(!user.getUpdatedAt().isBefore(previousUpdatedAt));
    }

    @Test
    void tenantProfileInitializesAndUpdatesAuditTimestamps() {
        TenantProfileEntity profile = new TenantProfileEntity();

        profile.initializeAuditTimestamps();

        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
        LocalDateTime previousUpdatedAt = profile.getUpdatedAt();

        profile.updateAuditTimestamp();

        assertTrue(!profile.getUpdatedAt().isBefore(previousUpdatedAt));
    }
}
