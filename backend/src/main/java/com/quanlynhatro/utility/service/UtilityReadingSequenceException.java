package com.quanlynhatro.utility.service;

import java.math.BigDecimal;

public class UtilityReadingSequenceException extends UtilityBusinessException {
    public UtilityReadingSequenceException(
            Long roomId,
            String utilityType,
            String period,
            BigDecimal expected,
            BigDecimal actual) {
        super(
                "ReadingStart does not match the previous ReadingEnd for room "
                        + roomId
                        + ", "
                        + utilityType
                        + ", "
                        + period
                        + ". Expected "
                        + expected
                        + " but was "
                        + actual);
    }
}
