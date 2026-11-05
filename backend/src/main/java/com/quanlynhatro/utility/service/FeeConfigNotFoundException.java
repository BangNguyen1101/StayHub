package com.quanlynhatro.utility.service;

public class FeeConfigNotFoundException extends UtilityBusinessException {
    public FeeConfigNotFoundException(Object value) {
        super("Fee configuration not found: " + value);
    }
}
