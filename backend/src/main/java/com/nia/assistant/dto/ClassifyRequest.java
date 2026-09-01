package com.nia.assistant.dto;

/** Request body for the AI service's /ai/classify endpoint. */
public record ClassifyRequest(String title, String description) {
}
