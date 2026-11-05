package com.quanlynhatro.utility.service;

public class ServiceNotFoundException extends UtilityBusinessException {
    public ServiceNotFoundException(Long id) {
        super("Service not found: " + id);
    }
}
