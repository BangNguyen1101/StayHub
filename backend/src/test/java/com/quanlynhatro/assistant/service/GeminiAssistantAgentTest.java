package com.quanlynhatro.assistant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlynhatro.assistant.api.AssistantChatRequest.Turn;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Runs the agent against a local fake of the Gemini generateContent endpoint. */
class GeminiAssistantAgentTest {
    private final ObjectMapper json = new ObjectMapper();
    private final Deque<String> replies = new ArrayDeque<>();
    private final List<JsonNode> requests = new ArrayList<>();
    private final List<String> apiKeys = new ArrayList<>();
    private HttpServer server;

    @BeforeEach
    void startFakeGemini() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1beta/models/test-model:generateContent",
                exchange -> {
                    requests.add(json.readTree(exchange.getRequestBody()));
                    apiKeys.add(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
                    byte[] body = replies.removeFirst().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
    }

    @AfterEach
    void stopFakeGemini() {
        server.stop(0);
    }

    @Test
    void callsToolsWithUserScopeAndReturnsFinalAnswer() {
        replies.add(
                """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"functionCall":{"name":"tenant_invoices","args":{}},"thoughtSignature":"sig-1"}
                ]}}]}""");
        replies.add(
                """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"text":"thinking...","thought":true},
                  {"text":"Bạn còn **1.250.000 đ** chưa trả."}
                ]}}]}""");
        List<String> toolCalls = new ArrayList<>();

        GeminiAssistantAgent.Result result =
                agent()
                        .answer(
                                "TENANT",
                                "Bằng",
                                List.of(
                                        new Turn("assistant", "Xin chào!"),
                                        new Turn("user", "Hợp đồng của tôi?"),
                                        new Turn("assistant", "Hợp đồng HD03...")),
                                "Tôi còn nợ bao nhiêu?",
                                (name, args) -> {
                                    toolCalls.add(name);
                                    return "Bạn còn 1.250.000 đ trên 1 hóa đơn";
                                });

        assertEquals("Bạn còn **1.250.000 đ** chưa trả.", result.answer());
        assertEquals(List.of(AssistantIntent.TENANT_INVOICES), result.usedIntents());
        assertEquals(List.of("tenant_invoices"), toolCalls);
        assertEquals(List.of("test-key", "test-key"), apiKeys);

        JsonNode first = requests.get(0);
        // Tenants are only offered tenant tools.
        String declared = first.path("tools").path(0).path("functionDeclarations").toString();
        assertTrue(declared.contains("tenant_invoices"));
        assertFalse(declared.contains("admin_"));
        // A leading bot greeting is dropped so the conversation starts with the user.
        assertEquals("user", first.path("contents").path(0).path("role").asText());
        assertEquals(3, first.path("contents").size());

        JsonNode followUp = requests.get(1).path("contents");
        JsonNode echoed = followUp.get(followUp.size() - 2);
        assertEquals("sig-1", echoed.path("parts").path(0).path("thoughtSignature").asText());
        JsonNode functionResponse = followUp.get(followUp.size() - 1).path("parts").path(0);
        assertEquals("tenant_invoices", functionResponse.path("functionResponse").path("name").asText());
        assertEquals(
                "Bạn còn 1.250.000 đ trên 1 hóa đơn",
                functionResponse.path("functionResponse").path("response").path("result").asText());
    }

    @Test
    void toolErrorsAreReportedToTheModelInsteadOfFailingTheChat() {
        replies.add(
                """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"functionCall":{"name":"admin_utilities","args":{"period":"tháng 13"}}}
                ]}}]}""");
        replies.add(
                """
                {"candidates":[{"content":{"role":"model","parts":[{"text":"Kỳ tháng không hợp lệ."}]}}]}""");

        GeminiAssistantAgent.Result result =
                agent()
                        .answer(
                                "ADMIN",
                                "Chủ trọ",
                                List.of(),
                                "Điện nước tháng 13?",
                                (name, args) -> {
                                    throw new IllegalArgumentException(
                                            "Kỳ tháng phải có dạng yyyy-MM");
                                });

        assertEquals("Kỳ tháng không hợp lệ.", result.answer());
        assertTrue(result.usedIntents().isEmpty());
        assertEquals(
                "Kỳ tháng phải có dạng yyyy-MM",
                requests.get(1)
                        .path("contents")
                        .path(2)
                        .path("parts")
                        .path(0)
                        .path("functionResponse")
                        .path("response")
                        .path("error")
                        .asText());
    }

    @Test
    void emptyAnswerIsAFailureSoTheServiceCanFallBack() {
        replies.add("{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}");

        assertThrows(
                IllegalStateException.class,
                () -> agent().answer("TENANT", "B", List.of(), "hi", (name, args) -> ""));
    }

    @Test
    void disabledWithoutApiKey() {
        assertFalse(new GeminiAssistantAgent(" ", "m", "http://localhost", json).enabled());
    }

    private GeminiAssistantAgent agent() {
        return new GeminiAssistantAgent(
                "test-key",
                "test-model",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta",
                json);
    }
}
