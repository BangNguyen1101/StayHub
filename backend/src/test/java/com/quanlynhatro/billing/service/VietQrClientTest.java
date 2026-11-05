package com.quanlynhatro.billing.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class VietQrClientTest {
    private static VietQrClient configuredClient() {
        VietQrClient client = new VietQrClient();
        client.bankCode = "ICB";
        client.bankAccount = "0123456789";
        client.accountName = "NGUYEN VAN A";
        return client;
    }

    @Test
    void createsQuickLinkWithInvoiceAmountAndContentWithoutCredentials() {
        VietQrClient client = configuredClient();

        VietQrClient.Result result =
                client.generate(new BigDecimal("3115000"), "INV-1", "TTINV202610AF071B0D");

        assertEquals("ICB", result.bankCode());
        assertEquals("0123456789", result.accountNumber());
        assertTrue(
                result.qrLink()
                        .startsWith("https://img.vietqr.io/image/ICB-0123456789-compact2.svg"));
        assertTrue(result.qrLink().contains("amount=3115000"));
        assertTrue(result.qrLink().contains("addInfo=TTINV202610AF071B0D"));
        assertTrue(result.qrLink().contains("accountName=NGUYEN+VAN+A"));
    }

    @Test
    void rejectsFractionalVndAmount() {
        VietQrClient client = configuredClient();
        assertThrows(
                PaymentConflictException.class,
                () -> client.generate(new BigDecimal("100.50"), "INV-1", "TTINV1"));
    }

    @Test
    void refusesToGenerateWhenReceivingAccountIsNotConfigured() {
        VietQrClient client = new VietQrClient();
        client.bankCode = "ICB";
        client.bankAccount = "";
        client.accountName = "";
        assertThrows(
                PaymentConflictException.class,
                () -> client.generate(new BigDecimal("100000"), "INV-1", "TTINV1"));
    }
}
