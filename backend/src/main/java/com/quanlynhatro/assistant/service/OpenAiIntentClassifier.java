package com.quanlynhatro.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class OpenAiIntentClassifier implements AssistantIntentClassifier {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private final String apiKey;
    private final String model;
    private final RestClient client;
    private final ObjectMapper json;

    public OpenAiIntentClassifier(
            @Value("${app.assistant.openai-api-key:}") String apiKey,
            @Value("${app.assistant.openai-model:gpt-6-luna}") String model,
            @Value("${app.assistant.openai-base-url:https://api.openai.com/v1}") String baseUrl,
            ObjectMapper json) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.json = json;
    }

    @Override
    public AssistantIntent classify(String message, String role) {
        if (!apiKey.isBlank()) {
            try {
                String allowed =
                        "ADMIN".equals(role)
                                ? "ADMIN_MONTHLY_DEBT, ADMIN_EXPIRING_CONTRACTS, ADMIN_OVERVIEW,"
                                      + " ADMIN_ROOMS, ADMIN_REVENUE, ADMIN_MAINTENANCE,"
                                      + " ADMIN_UTILITIES, UNSUPPORTED"
                                : "TENANT_INVOICES, TENANT_UTILITIES, TENANT_CONTRACT,"
                                      + " TENANT_PAYMENT_DUE, TENANT_PAYMENTS, TENANT_MAINTENANCE,"
                                      + " UNSUPPORTED";
                String instructions =
                        "Phân loại câu hỏi tiếng Việt về quản lý nhà trọ. Chỉ trả về đúng một nhãn,"
                            + " không giải thích. Nhãn hợp lệ: "
                                + allowed
                                + ". Không được chọn nhãn ADMIN cho người dùng TENANT.";
                String body =
                        client.post()
                                .uri("/responses")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                                .body(
                                        Map.of(
                                                "model",
                                                model,
                                                "instructions",
                                                instructions,
                                                "input",
                                                message,
                                                "max_output_tokens",
                                                30))
                                .retrieve()
                                .body(String.class);
                AssistantIntent parsed = parseResponse(body);
                if (allowed.contains(parsed.name())) return parsed;
            } catch (RuntimeException ignored) {
                // A deterministic classifier keeps supported factual questions available
                // during a temporary OpenAI outage without inventing any business data.
            }
        }
        return localFallback(message, role);
    }

    private AssistantIntent parseResponse(String body) {
        try {
            JsonNode output = json.readTree(body).path("output");
            for (JsonNode item : output)
                for (JsonNode content : item.path("content")) {
                    String text = content.path("text").asText("").trim();
                    if (!text.isEmpty()) return AssistantIntent.valueOf(text);
                }
        } catch (Exception ignored) {
        }
        return AssistantIntent.UNSUPPORTED;
    }

    private AssistantIntent localFallback(String message, String role) {
        String text =
                DIACRITICS
                        .matcher(Normalizer.normalize(message, Normalizer.Form.NFD))
                        .replaceAll("")
                        .toLowerCase(Locale.ROOT);
        if ("ADMIN".equals(role)) {
            if (text.contains("tong quan")
                    || text.contains("tinh hinh nha tro")
                    || text.contains("hom nay co gi")) return AssistantIntent.ADMIN_OVERVIEW;
            if (text.contains("doanh thu") || text.contains("da thu duoc"))
                return AssistantIntent.ADMIN_REVENUE;
            if (text.contains("bao tri") || text.contains("su co") || text.contains("sua chua"))
                return AssistantIntent.ADMIN_MAINTENANCE;
            if (text.contains("dien nuoc")
                    || text.contains("tieu thu dien")
                    || text.contains("tieu thu nuoc")) return AssistantIntent.ADMIN_UTILITIES;
            if (text.contains("phong trong")
                    || text.contains("tinh trang phong")
                    || text.contains("bao nhieu phong")) return AssistantIntent.ADMIN_ROOMS;
            if (text.contains("no")
                    || text.contains("chua thu")
                    || text.contains("con bao nhieu tien"))
                return AssistantIntent.ADMIN_MONTHLY_DEBT;
            if (text.contains("het hop dong")
                    || text.contains("sap het")
                    || text.contains("dao han")) return AssistantIntent.ADMIN_EXPIRING_CONTRACTS;
        } else {
            if (text.contains("bao tri") || text.contains("su co") || text.contains("sua chua"))
                return AssistantIntent.TENANT_MAINTENANCE;
            if (text.contains("lich su thanh toan")
                    || text.contains("da thanh toan")
                    || text.contains("giao dich")) return AssistantIntent.TENANT_PAYMENTS;
            if (text.contains("dien") || text.contains("nuoc"))
                return AssistantIntent.TENANT_UTILITIES;
            if (text.contains("han thanh toan")
                    || text.contains("den han")
                    || text.contains("qua han")) return AssistantIntent.TENANT_PAYMENT_DUE;
            if (text.contains("hop dong")) return AssistantIntent.TENANT_CONTRACT;
            if (text.contains("hoa don") || text.contains("con no") || text.contains("phai tra"))
                return AssistantIntent.TENANT_INVOICES;
        }
        return AssistantIntent.UNSUPPORTED;
    }
}
