package com.quanlynhatro.billing.repository;

import com.quanlynhatro.billing.domain.InvoiceItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity, Long> {
    List<InvoiceItemEntity> findByInvoice_IdOrderByIdAsc(Long invoiceId);

    boolean existsByUtilityReading_Id(Long readingId);

    void deleteByInvoice_Id(Long invoiceId);
}
