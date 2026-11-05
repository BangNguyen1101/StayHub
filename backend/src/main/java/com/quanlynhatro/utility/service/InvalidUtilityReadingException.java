package com.quanlynhatro.utility.service;

public class InvalidUtilityReadingException extends UtilityBusinessException {
    public InvalidUtilityReadingException() {
        super("ReadingEnd must be greater than or equal to ReadingStart");
    }
}
