package com.quanlynhatro.utility.service;

public class DuplicateUtilityReadingException extends UtilityBusinessException {
    public DuplicateUtilityReadingException(Long roomId, String utilityType, String period) {
        super(
                "Utility reading already exists for room "
                        + roomId
                        + ", "
                        + utilityType
                        + ", "
                        + period);
    }
}
