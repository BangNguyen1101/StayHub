package com.quanlynhatro.billing.webhook;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Safe default: prevents accidental activation without provider documentation/configuration. */
@Component
@Profile("!bank-webhook-enabled")
public final class DisabledBankReconciliationProvider implements BankReconciliationProvider {
    public String providerCode() {
        return "DISABLED";
    }

    public WebhookVerificationResult verify(String rawBody, Map<String, String> headers) {
        return new WebhookVerificationResult(false, null, null, null, null, null, null, "DISABLED");
    }
}
