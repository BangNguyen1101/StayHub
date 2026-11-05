package com.quanlynhatro.tenant.service;

public class TenantAccountConflictException extends RuntimeException {
    public TenantAccountConflictException() {
        super("Username or email already exists");
    }
}
