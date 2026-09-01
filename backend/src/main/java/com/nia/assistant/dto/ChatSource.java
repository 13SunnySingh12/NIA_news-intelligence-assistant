package com.nia.assistant.dto;

import java.util.UUID;

/** A trusted supporting article for an assistant answer. The URL comes from the DB, never the LLM. */
public record ChatSource(UUID id, String title, String source, String url) {
}
