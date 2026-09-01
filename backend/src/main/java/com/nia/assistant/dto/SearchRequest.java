package com.nia.assistant.dto;

/** Semantic search request forwarded to FastAPI. */
public record SearchRequest(String query, int topK, SearchFilters filters) {

    public record SearchFilters(String category, String language, String publishedAfter) {
    }
}
