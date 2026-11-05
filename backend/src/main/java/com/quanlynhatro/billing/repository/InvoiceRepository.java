package com.quanlynhatro.billing.repository;

import com.quanlynhatro.billing.api.InvoiceStatus;
import com.quanlynhatro.billing.domain.InvoiceEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    boolean existsByContract_Id(Long contractId);

    boolean existsByContract_IdAndBillingPeriod(Long contractId, String billingPeriod);

    boolean existsByContract_IdAndBillingPeriodAndStatusNot(
            Long contractId, String billingPeriod, InvoiceStatus status);

    boolean existsByInvoiceCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvoiceEntity i where i.id=:id")
    Optional<InvoiceEntity> findByIdForUpdate(@Param("id") Long id);

    @Query(
            "select i from InvoiceEntity i where (:keyword is null or :keyword='' or"
                + " lower(i.invoiceCode) like lower(concat('%',:keyword,'%')) or"
                + " lower(i.contract.contractCode) like lower(concat('%',:keyword,'%')) or"
                + " lower(i.payerUser.username) like lower(concat('%',:keyword,'%')) or"
                + " lower(i.payerUser.fullName) like lower(concat('%',:keyword,'%')) or"
                + " lower(i.room.roomCode) like lower(concat('%',:keyword,'%'))) and (:period is"
                + " null or i.billingPeriod=:period) and (:status is null or i.status=:status) and"
                + " (:contractId is null or i.contract.id=:contractId) and (:payerId is null or"
                + " i.payerUser.id=:payerId)")
    Page<InvoiceEntity> search(
            @Param("keyword") String keyword,
            @Param("period") String period,
            @Param("status") InvoiceStatus status,
            @Param("contractId") Long contractId,
            @Param("payerId") Long payerId,
            Pageable pageable);

    /** A tenant's invoices; drafts are internal to the admin until they are issued. */
    @Query(
            "select i from InvoiceEntity i where i.payerUser.id=:payerId and"
                    + " i.status <> com.quanlynhatro.billing.api.InvoiceStatus.DRAFT")
    Page<InvoiceEntity> searchIssuedForPayer(@Param("payerId") Long payerId, Pageable pageable);

    @Query(
            "select i.id from InvoiceEntity i where"
                    + " i.status = com.quanlynhatro.billing.api.InvoiceStatus.DRAFT and (:period is"
                    + " null or i.billingPeriod=:period) order by i.id")
    List<Long> findDraftIds(@Param("period") String period);

    @Query(
            "select i from InvoiceEntity i where i.totalAmount > coalesce((select sum(p.amount)"
                + " from PaymentEntity p where p.invoice.id=i.id and"
                + " p.status='SUCCEEDED'),i.paidAmount) and i.status not in ('DRAFT','CANCELLED')"
                + " and (:keyword is null or :keyword='' or lower(i.invoiceCode) like"
                + " lower(concat('%',:keyword,'%')) or lower(i.payerUser.username) like"
                + " lower(concat('%',:keyword,'%')) or lower(i.payerUser.fullName) like"
                + " lower(concat('%',:keyword,'%'))) and (:payerId is null or"
                + " i.payerUser.id=:payerId or exists (select o.id from ContractOccupantEntity o"
                + " where o.contract=i.contract and o.user.id=:payerId and o.moveInDate <="
                + " current_date and (o.moveOutDate is null or o.moveOutDate >= current_date)))")
    Page<InvoiceEntity> searchOutstanding(
            @Param("keyword") String keyword, @Param("payerId") Long payerId, Pageable pageable);
}
