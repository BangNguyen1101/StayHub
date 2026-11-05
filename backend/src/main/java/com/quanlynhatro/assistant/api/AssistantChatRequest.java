package com.quanlynhatro.assistant.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param history the most recent turns of this chat (oldest first), so follow-up questions such as
 *     "còn tháng trước?" keep their context; it is supplied by the client and never trusted for
 *     authorization
 */
public record AssistantChatRequest(
        @NotBlank @Size(max = 1000) String message,
        @Valid @Size(max = 12) List<Turn> history) {

    public record Turn(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 4000) String text) {}

    public List<Turn> historyOrEmpty() {
        return history == null ? List.of() : history;
    }
}
