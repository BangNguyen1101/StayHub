package com.quanlynhatro.contract.repository;

import com.quanlynhatro.contract.api.ContractDocumentStatus;
import com.quanlynhatro.contract.api.ContractDocumentType;
import com.quanlynhatro.contract.domain.ContractDocumentEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractDocumentRepository extends JpaRepository<ContractDocumentEntity, Long> {
    List<ContractDocumentEntity> findByContract_IdOrderByCreatedAtDesc(Long contractId);

    Optional<ContractDocumentEntity> findByContract_IdAndStatus(
            Long contractId, ContractDocumentStatus status);

    boolean existsByContract_IdAndDocumentTypeAndStatus(
            Long contractId, ContractDocumentType documentType, ContractDocumentStatus status);

    boolean existsByContract_IdAndStatus(Long contractId, ContractDocumentStatus status);

    void deleteByContract_Id(Long contractId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from ContractDocumentEntity d where d.id = :id")
    Optional<ContractDocumentEntity> findByIdForUpdate(@Param("id") Long id);
}
