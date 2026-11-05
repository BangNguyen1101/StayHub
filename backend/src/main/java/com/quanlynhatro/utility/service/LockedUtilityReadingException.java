package com.quanlynhatro.utility.service;

public class LockedUtilityReadingException extends UtilityBusinessException {
    public LockedUtilityReadingException(Long id) {
        super("Utility reading is locked: " + id);
    }
}
