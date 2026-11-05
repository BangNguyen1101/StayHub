package com.quanlynhatro.assistant.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param intent the main data lookup used for the answer
 * @param actions links to the app pages related to the answer
 * @param suggestions follow-up questions the user can tap
 * @param mode {@code AI} when Gemini wrote the answer, {@code BASIC} for the keyword fallback
 */
public record AssistantChatResponse(
        String answer,
        String intent,
        LocalDateTime answeredAt,
        List<Action> actions,
        List<String> suggestions,
        String mode) {

    public record Action(String label, String path) {}
}
