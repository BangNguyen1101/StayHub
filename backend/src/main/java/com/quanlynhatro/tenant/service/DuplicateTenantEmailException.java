package com.quanlynhatro.tenant.service;

public class DuplicateTenantEmailException extends RuntimeException {
    public DuplicateTenantEmailException(String email) {
        super("Tenant email already exists: " + email);
    }
}
