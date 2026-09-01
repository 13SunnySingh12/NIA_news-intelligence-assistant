package com.nia.assistant.dto;

import java.util.List;
import java.util.UUID;

/** Batch of newly-ingested articles for FastAPI to embed. */
public record EmbedRequest(List<EmbedItem> articles) {

    public record EmbedItem(UUID id, String text) {
    }
}
