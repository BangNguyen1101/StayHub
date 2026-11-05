package com.quanlynhatro.contract.repository;

import com.quanlynhatro.contract.domain.ContractHistoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractHistoryRepository extends JpaRepository<ContractHistoryEntity, Long> {
    void deleteByContract_Id(Long contractId);

    List<ContractHistoryEntity> findByContract_IdOrderByPerformedAtDesc(Long contractId);
}
