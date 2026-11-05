package com.quanlynhatro.billing.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.billing.api.*;
import com.quanlynhatro.billing.domain.InvoiceEntity;
import com.quanlynhatro.billing.domain.PaymentEntity;
import com.quanlynhatro.billing.repository.InvoiceRepository;
import com.quanlynhatro.billing.repository.PaymentRepository;
import com.quanlynhatro.contract.domain.SystemConfigEntity;
import com.quanlynhatro.contract.repository.SystemConfigRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock InvoiceRepository invoices;
    @Mock AppUserRepository users;
    @Mock SystemConfigRepository configs;

    @Test
    void manualPaymentAcceptsPartialAmountAndUsesInvoiceLock() {
        PaymentServiceImpl service = new PaymentServiceImpl(payments, invoices, users, configs);
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        AppUserEntity admin = mock(AppUserEntity.class);
        AppUserEntity payer = mock(AppUserEntity.class);
        when(invoices.findByIdForUpdate(1L)).thenReturn(Optional.of(invoice));
        when(users.findById(9L)).thenReturn(Optional.of(admin));
        when(invoice.getStatus()).thenReturn(InvoiceStatus.UNPAID);
        when(invoice.getTotalAmount()).thenReturn(new BigDecimal("100.00"));
        when(invoice.getPayerUser()).thenReturn(payer);
        when(invoice.getId()).thenReturn(1L);
        when(payments.existsByTransactionCodeIgnoreCase("TX-1")).thenReturn(false);
        when(payments.sumAmount(1L, PaymentStatus.SUCCEEDED)).thenReturn(new BigDecimal("40.00"));
        when(payments.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        service.recordManual(
                1L, new ManualPaymentRequest("TX-1", new BigDecimal("20.00"), "BANK_TRANSFER"), 9L);
        verify(invoices).findByIdForUpdate(1L);
        verify(invoice).applyPayment(eq(new BigDecimal("60.00")), any());
    }

    @Test
    void manualPaymentBlocksAmountAboveOutstanding() {
        PaymentServiceImpl service = new PaymentServiceImpl(payments, invoices, users, configs);
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        when(invoices.findByIdForUpdate(1L)).thenReturn(Optional.of(invoice));
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));
        when(invoice.getStatus()).thenReturn(InvoiceStatus.UNPAID);
        when(invoice.getTotalAmount()).thenReturn(new BigDecimal("100.00"));
        when(invoice.getId()).thenReturn(1L);
        when(payments.existsByTransactionCodeIgnoreCase(anyString())).thenReturn(false);
        when(payments.sumAmount(1L, PaymentStatus.SUCCEEDED)).thenReturn(new BigDecimal("90.00"));
        assertThrows(
                PaymentConflictException.class,
                () ->
                        service.recordManual(
                                1L,
                                new ManualPaymentRequest(
                                        "TX-2", new BigDecimal("11.00"), "BANK_TRANSFER"),
                                9L));
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void duplicateTransactionCodeIsRejected() {
        PaymentServiceImpl service = new PaymentServiceImpl(payments, invoices, users, configs);
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        when(invoices.findByIdForUpdate(1L)).thenReturn(Optional.of(invoice));
        when(users.findById(9L)).thenReturn(Optional.of(mock(AppUserEntity.class)));
        when(invoice.getStatus()).thenReturn(InvoiceStatus.UNPAID);
        when(payments.existsByTransactionCodeIgnoreCase("TX-1")).thenReturn(true);
        assertThrows(
                PaymentConflictException.class,
                () ->
                        service.recordManual(
                                1L,
                                new ManualPaymentRequest("TX-1", BigDecimal.ONE, "BANK_TRANSFER"),
                                9L));
    }

    @Test
    void qrIsRefusedForDraftInvoice() {
        PaymentServiceImpl service = new PaymentServiceImpl(payments, invoices, users, configs);
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        AppUserEntity payer = mock(AppUserEntity.class);
        when(invoices.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoice.getPayerUser()).thenReturn(payer);
        when(payer.getId()).thenReturn(10L);
        when(invoice.getStatus()).thenReturn(com.quanlynhatro.billing.api.InvoiceStatus.DRAFT);

        assertThrows(PaymentConflictException.class, () -> service.qrForTenant(1L, 10L));
        verify(payments, never()).save(any(PaymentEntity.class));
    }

    @Test
    void qrRequiresConfiguredBankDetailsAndDoesNotCreatePayment() {
        PaymentServiceImpl service = new PaymentServiceImpl(payments, invoices, users, configs);
        InvoiceEntity invoice = mock(InvoiceEntity.class);
        AppUserEntity payer = mock(AppUserEntity.class);
        SystemConfigEntity cfg = mock(SystemConfigEntity.class);
        when(invoices.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoice.getPayerUser()).thenReturn(payer);
        when(payer.getId()).thenReturn(10L);
        when(invoice.getTotalAmount()).thenReturn(new BigDecimal("100.00"));
        when(invoice.getInvoiceCode()).thenReturn("INV-1");
        when(payments.sumAmount(1L, PaymentStatus.SUCCEEDED)).thenReturn(BigDecimal.ZERO);
        when(configs.findById("PAYMENT_BANK_CODE")).thenReturn(Optional.of(cfg));
        when(cfg.getValue()).thenReturn("");
        assertThrows(PaymentConflictException.class, () -> service.qrForTenant(1L, 10L));
        verify(payments, never()).save(any(PaymentEntity.class));
    }
}
