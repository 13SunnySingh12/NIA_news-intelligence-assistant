package com.nia.assistant.dto;

/** Result of a bounded pending-embed pass: how many were embedded, and how many remain. */
public record EmbedPendingResponse(int embedded, int remaining) {
}
