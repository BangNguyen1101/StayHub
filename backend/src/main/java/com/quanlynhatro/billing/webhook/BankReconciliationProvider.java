package com.quanlynhatro.billing.webhook;

import java.math.BigDecimal;

/**
 * Provider-neutral contract. Implement only after the provider's official webhook specification is
 * supplied.
 */
public interface BankReconciliationProvider {
    String providerCode();

    WebhookVerificationResult verify(String rawBody, java.util.Map<String, String> headers);

    record WebhookVerificationResult(
            boolean signatureValid,
            String eventId,
            String transactionId,
            String receivingAccount,
            String transferContent,
            BigDecimal amount,
            String currency,
            String status) {}
}
