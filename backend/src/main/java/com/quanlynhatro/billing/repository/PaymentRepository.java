package com.quanlynhatro.billing.repository;

import com.quanlynhatro.billing.api.PaymentStatus;
import com.quanlynhatro.billing.domain.PaymentEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.*;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    boolean existsByTransactionCodeIgnoreCase(String code);

    Optional<PaymentEntity> findByTransactionCodeIgnoreCase(String code);

    boolean existsByInvoice_IdAndStatus(Long invoiceId, PaymentStatus status);

    @Query(
            "select coalesce(sum(p.amount),0) from PaymentEntity p where p.invoice.id=:invoiceId"
                + " and p.status=:status")
    BigDecimal sumAmount(@Param("invoiceId") Long invoiceId, @Param("status") PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.id=:id")
    Optional<PaymentEntity> findByIdForUpdate(@Param("id") Long id);

    List<PaymentEntity> findByInvoice_IdOrderByCreatedAtDesc(Long invoiceId);

    Page<PaymentEntity> findByPayerUser_Id(Long userId, Pageable pageable);

    @Query(
            "select p from PaymentEntity p where (:status is null or p.status=:status) and"
                + " (:provider is null or p.providerCode=:provider) and (:keyword is null or"
                + " :keyword='' or lower(p.transactionCode) like lower(concat('%',:keyword,'%')) or"
                + " lower(p.providerTransactionCode) like lower(concat('%',:keyword,'%')))")
    Page<PaymentEntity> search(
            String keyword, String provider, PaymentStatus status, Pageable pageable);
}
