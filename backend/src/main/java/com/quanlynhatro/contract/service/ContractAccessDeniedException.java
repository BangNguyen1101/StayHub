package com.quanlynhatro.contract.service;

public class ContractAccessDeniedException extends RuntimeException {
    public ContractAccessDeniedException() {
        super("You do not have access to this contract");
    }
}
