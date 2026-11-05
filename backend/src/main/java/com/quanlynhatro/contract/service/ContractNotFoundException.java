package com.quanlynhatro.contract.service;

public class ContractNotFoundException extends RuntimeException {
    public ContractNotFoundException(Long id) {
        super("Contract not found: " + id);
    }
}
