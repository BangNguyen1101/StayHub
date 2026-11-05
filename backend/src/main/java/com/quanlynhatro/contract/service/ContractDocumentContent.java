package com.quanlynhatro.contract.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Frozen content of a contract document, stored as JSON when the document is issued. The PDF is
 * rendered only from this snapshot, so what the tenant previewed is exactly what gets signed.
 */
public record ContractDocumentContent(
        String documentType,
        String contractCode,
        LocalDate issuedDate,
        Party landlord,
        Party tenant,
        RoomInfo room,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        BigDecimal depositAmount,
        Integer billingCycleMonths,
        Integer dueDay,
        String terms,
        LocalDate previousEndDate,
        LocalDate newEndDate,
        Integer renewalMonths) {

    public record Party(
            String fullName,
            String phoneNumber,
            String email,
            String identityDocumentType,
            String identityDocumentNumber,
            LocalDate identityIssuedDate,
            String identityIssuedPlace,
            String address) {}

    public record RoomInfo(
            String roomCode, String roomName, Integer floorNumber, BigDecimal areaM2) {}
}
