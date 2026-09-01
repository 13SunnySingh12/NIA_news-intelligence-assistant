package com.nia.assistant.dto;

/** One turn of short client-side conversation history. Role is "user" or "assistant". */
public record ChatMessage(String role, String content) {
}
