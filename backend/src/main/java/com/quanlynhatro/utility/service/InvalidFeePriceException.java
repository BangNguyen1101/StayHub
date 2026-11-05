package com.quanlynhatro.utility.service;

public class InvalidFeePriceException extends UtilityBusinessException {
    public InvalidFeePriceException() {
        super("Unit price must not be negative");
    }
}
