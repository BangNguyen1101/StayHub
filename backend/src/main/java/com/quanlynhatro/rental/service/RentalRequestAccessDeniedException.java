package com.quanlynhatro.rental.service;

public class RentalRequestAccessDeniedException extends RuntimeException {
    public RentalRequestAccessDeniedException() {
        super("The current user is not allowed to perform this operation");
    }
}
