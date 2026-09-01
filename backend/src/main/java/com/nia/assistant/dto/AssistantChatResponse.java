package com.nia.assistant.dto;

import java.util.List;

/** Grounded assistant answer with its supporting sources. */
public record AssistantChatResponse(String answer, List<ChatSource> sources) {
}
