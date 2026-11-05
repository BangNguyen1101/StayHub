package com.quanlynhatro.utility.service;

public class UtilityReadingNotFoundException extends UtilityBusinessException {
    public UtilityReadingNotFoundException(Long id) {
        super("Utility reading not found: " + id);
    }
}
