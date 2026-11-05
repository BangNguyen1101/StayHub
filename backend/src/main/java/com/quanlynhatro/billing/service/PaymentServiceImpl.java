package com.quanlynhatro.billing.service;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.api.*;
import com.quanlynhatro.billing.domain.PaymentEntity;
import com.quanlynhatro.billing.repository.InvoiceRepository;
import com.quanlynhatro.billing.repository.PaymentRepository;
import com.quanlynhatro.contract.repository.SystemConfigRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final AppUserRepository users;
    private final VietQrClient vietQr;

    @Autowired
    public PaymentServiceImpl(
            PaymentRepository payments,
            InvoiceRepository invoices,
            AppUserRepository users,
            SystemConfigRepository configs,
            VietQrClient vietQr) {
        this.payments = payments;
        this.invoices = invoices;
        this.users = users;
        this.vietQr = vietQr;
    }

    /** Compatibility constructor used by older unit tests. Spring uses the constructor above. */
    public PaymentServiceImpl(
            PaymentRepository payments,
            InvoiceRepository invoices,
            AppUserRepository users,
            SystemConfigRepository configs) {
        this.payments = payments;
        this.invoices = invoices;
        this.users = users;
        this.vietQr = null;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentResponse recordManual(
            Long invoiceId, ManualPaymentRequest request, Long adminId) {
        var invoice =
                invoices.findByIdForUpdate(invoiceId)
                        .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        AppUserEntity admin =
                users.findById(adminId).orElseThrow(InvoiceAccessDeniedException::new);
        if (invoice.getStatus() == InvoiceStatus.DRAFT
                || invoice.getStatus() == InvoiceStatus.CANCELLED)
            throw new PaymentConflictException("Invoice cannot receive payment in current status");
        if (payments.existsByTransactionCodeIgnoreCase(request.transactionCode()))
            throw new PaymentConflictException("Transaction code already exists");
        BigDecimal paid =
                Optional.ofNullable(payments.sumAmount(invoiceId, PaymentStatus.SUCCEEDED))
                        .orElse(BigDecimal.ZERO);
        BigDecimal outstanding = invoice.getTotalAmount().subtract(paid);
        if (request.amount().compareTo(outstanding) > 0)
            throw new PaymentConflictException("Payment exceeds outstanding balance");
        PaymentEntity payment =
                PaymentEntity.manual(
                        request.transactionCode(),
                        invoice,
                        invoice.getPayerUser(),
                        request.amount(),
                        request.paymentMethod(),
                        admin);
        payments.saveAndFlush(payment);
        invoice.applyPayment(paid.add(request.amount()), LocalDate.now());
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentQrResponse qrForTenant(Long invoiceId, Long tenantId) {
        var invoice =
                invoices.findById(invoiceId)
                        .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        if (!Objects.equals(invoice.getPayerUser().getId(), tenantId))
            throw new InvoiceAccessDeniedException();
        if (invoice.getStatus() == InvoiceStatus.DRAFT)
            throw new PaymentConflictException("Hoá đơn chưa được phát hành, chưa thể thanh toán");
        if (invoice.getStatus() == InvoiceStatus.CANCELLED)
            throw new PaymentConflictException("Hoá đơn đã bị huỷ");
        BigDecimal paid =
                Optional.ofNullable(payments.sumAmount(invoiceId, PaymentStatus.SUCCEEDED))
                        .orElse(BigDecimal.ZERO);
        BigDecimal amount = invoice.getTotalAmount().subtract(paid);
        if (amount.signum() <= 0)
            throw new PaymentConflictException("Hoá đơn này đã được thanh toán đủ");
        String content = ("TT" + invoice.getInvoiceCode()).replaceAll("[^A-Za-z0-9]", "");
        if (content.length() > 50) content = content.substring(0, 50);
        if (vietQr == null)
            throw new PaymentConflictException("Chủ trọ chưa cấu hình tài khoản nhận tiền VietQR, vui lòng liên hệ quản lý");
        VietQrClient.Result result = vietQr.generate(amount, "INV" + invoice.getId(), content);
        return new PaymentQrResponse(
                invoiceId,
                invoice.getInvoiceCode(),
                amount,
                "VND",
                result.bankCode(),
                result.accountNumber(),
                result.accountName(),
                content,
                result.qrLink());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchAdmin(PaymentQuery query, Pageable pageable) {
        return payments.search(query.keyword(), query.providerCode(), query.status(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchTenant(Long tenantId, Pageable pageable) {
        return payments.findByPayerUser_Id(tenantId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> invoiceHistory(
            Long invoiceId, Long tenantId, boolean admin, Pageable pageable) {
        var invoice =
                invoices.findById(invoiceId)
                        .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        if (!admin && !Objects.equals(invoice.getPayerUser().getId(), tenantId))
            throw new InvoiceAccessDeniedException();
        return new PageImpl<>(
                        payments.findByInvoice_IdOrderByCreatedAtDesc(invoiceId),
                        pageable,
                        payments.findByInvoice_IdOrderByCreatedAtDesc(invoiceId).size())
                .map(this::toResponse);
    }

    private PaymentResponse toResponse(com.quanlynhatro.billing.domain.PaymentEntity p) {
        return new PaymentResponse(
                p.getId(),
                p.getTransactionCode(),
                p.getInvoice().getId(),
                p.getPayerUser().getId(),
                p.getProviderCode(),
                p.getPaymentMethod(),
                p.getAmount(),
                p.getCurrencyCode(),
                p.getStatus(),
                p.getProviderTransactionCode(),
                p.getConfirmedAt());
    }
}
