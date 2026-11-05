package com.quanlynhatro.billing.webhook;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Map;

class DisabledBankReconciliationProviderTest {
    @Test
    void providerDoesNotVerifyUnknownPayload() {
        var result = new DisabledBankReconciliationProvider().verify("{}", Map.of());
        assertFalse(result.signatureValid());
        assertEquals("DISABLED", result.status());
        assertNull(result.transactionId());
    }
}
