package com.quanlynhatro.utility.service;

public class DuplicateServiceCodeException extends UtilityBusinessException {
    public DuplicateServiceCodeException(String code) {
        super("Service code already exists: " + code);
    }
}
