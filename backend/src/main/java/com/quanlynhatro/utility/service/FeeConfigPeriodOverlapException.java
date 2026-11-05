package com.quanlynhatro.utility.service;

public class FeeConfigPeriodOverlapException extends UtilityBusinessException {
    public FeeConfigPeriodOverlapException(String feeCode) {
        super("Fee configuration period overlaps for: " + feeCode);
    }
}
