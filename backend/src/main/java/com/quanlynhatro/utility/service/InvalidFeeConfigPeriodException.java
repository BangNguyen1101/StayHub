package com.quanlynhatro.utility.service;

public class InvalidFeeConfigPeriodException extends UtilityBusinessException {
    public InvalidFeeConfigPeriodException() {
        super("EffectiveTo must be greater than or equal to EffectiveFrom");
    }
}
