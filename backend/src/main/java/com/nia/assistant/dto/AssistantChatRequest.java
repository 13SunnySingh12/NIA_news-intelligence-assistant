package com.nia.assistant.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Assistant question plus optional short conversation history (used both inbound and to FastAPI). */
public record AssistantChatRequest(
        @NotBlank String question,
        List<ChatMessage> conversation
) {
}
