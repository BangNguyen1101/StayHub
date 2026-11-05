package com.quanlynhatro.assistant.api;

import com.quanlynhatro.assistant.service.AssistantService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(
            Authentication authentication, @Valid @RequestBody AssistantChatRequest request) {
        return assistant.chat(authentication, request.message(), request.historyOrEmpty());
    }
}
