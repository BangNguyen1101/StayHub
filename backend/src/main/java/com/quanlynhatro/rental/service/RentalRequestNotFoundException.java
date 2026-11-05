package com.quanlynhatro.rental.service;

public class RentalRequestNotFoundException extends RuntimeException {
    public RentalRequestNotFoundException(Long id) {
        super("Rental request not found: " + id);
    }
}
