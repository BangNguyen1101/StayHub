package com.quanlynhatro.maintenance.service;

public class MaintenanceNotFoundException extends MaintenanceBusinessException {
    public MaintenanceNotFoundException() {
        super("Maintenance request not found");
    }
}
