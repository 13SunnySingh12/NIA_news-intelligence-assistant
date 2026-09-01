package com.nia.assistant.dto;

import java.util.List;
import java.util.UUID;

/** Ranked article ids from semantic search. Scores are internal and never shown to users. */
public record SearchResponse(List<SearchHit> results) {

    public record SearchHit(UUID id, Double score) {
    }
}
