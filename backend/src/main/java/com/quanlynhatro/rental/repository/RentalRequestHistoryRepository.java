package com.quanlynhatro.rental.repository;

import com.quanlynhatro.rental.domain.RentalRequestHistoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalRequestHistoryRepository
        extends JpaRepository<RentalRequestHistoryEntity, Long> {

    List<RentalRequestHistoryEntity> findByRentalRequest_IdOrderByChangedAtDesc(
            Long rentalRequestId);
}
