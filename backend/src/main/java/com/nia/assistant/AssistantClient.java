package com.nia.assistant;

import com.nia.assistant.dto.AssistantChatRequest;
import com.nia.assistant.dto.AssistantChatResponse;
import com.nia.assistant.dto.ClassifyRequest;
import com.nia.assistant.dto.ClassifyResponse;
import com.nia.assistant.dto.EmbedPendingResponse;
import com.nia.assistant.dto.EmbedRequest;
import com.nia.assistant.dto.SearchRequest;
import com.nia.assistant.dto.SearchResponse;
import com.nia.assistant.dto.SummarizeRequest;
import com.nia.assistant.dto.SummarizeResponse;
import com.nia.common.ApiException;
import com.nia.config.InternalClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The single gateway from Spring Boot to the internal FastAPI AI service.
 * Every call carries the shared internal token (configured on the WebClient).
 * Failures are turned into user-safe messages; provider/model details never leak.
 */
@Component
public class AssistantClient {

    private static final Logger log = LoggerFactory.getLogger(AssistantClient.class);

    private final WebClient client;

    public AssistantClient(@Qualifier(InternalClientConfig.FASTAPI_CLIENT) WebClient client) {
        this.client = client;
    }

    public AssistantChatResponse chat(AssistantChatRequest request) {
        try {
            return client.post().uri("/ai/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AssistantChatResponse.class)
                    .block();
        } catch (Exception ex) {
            log.warn("Assistant chat failed: {}", ex.getMessage());
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "assistant_unavailable",
                    "The AI assistant is unavailable right now — please try again in a few minutes.");
        }
    }

    public SummarizeResponse summarize(SummarizeRequest request) {
        try {
            return client.post().uri("/ai/summarize")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SummarizeResponse.class)
                    .block();
        } catch (Exception ex) {
            log.warn("Summarize failed: {}", ex.getMessage());
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "summary_unavailable",
                    "Couldn't generate a summary right now — please try again in a few minutes.");
        }
    }

    /** Semantic search. Throws {@link AiUnavailableException} so callers can fall back to keyword search. */
    public SearchResponse search(SearchRequest request) {
        try {
            return client.post().uri("/ai/search")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SearchResponse.class)
                    .block();
        } catch (Exception ex) {
            log.warn("Semantic search failed: {}", ex.getMessage());
            throw new AiUnavailableException("semantic search unavailable", ex);
        }
    }

    /** Trigger embedding for newly-ingested articles. Never throws — embeddings are best-effort. */
    public void embed(EmbedRequest request) {
        if (request.articles() == null || request.articles().isEmpty()) {
            return;
        }
        try {
            client.post().uri("/ai/embed")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            // Embeddings can be backfilled later; ingestion must not fail because of this.
            log.warn("Embedding request failed for {} articles: {}", request.articles().size(), ex.getMessage());
        }
    }

    /**
     * Embed up to {@code limit} not-yet-embedded articles. Bounded per call so
     * embedding stays within the provider's rate limit; the backlog is worked off
     * over successive ingestion cycles. Best-effort — never throws.
     */
    public int embedPending(int limit) {
        try {
            EmbedPendingResponse response = client.post().uri("/ai/embed/pending")
                    .bodyValue(java.util.Map.of("limit", limit))
                    .retrieve()
                    .bodyToMono(EmbedPendingResponse.class)
                    .block();
            return response != null ? response.embedded() : 0;
        } catch (Exception ex) {
            log.warn("Pending embed failed: {}", ex.getMessage());
            return 0;
        }
    }

    /**
     * Classify an article into a NIA category (Groq). Best-effort: returns
     * {@code null} when the AI service is unavailable, so ingestion never fails
     * because classification did.
     */
    public String classify(String title, String description) {
        try {
            ClassifyResponse response = client.post().uri("/ai/classify")
                    .bodyValue(new ClassifyRequest(title, description))
                    .retrieve()
                    .bodyToMono(ClassifyResponse.class)
                    .block();
            return response != null ? response.category() : null;
        } catch (Exception ex) {
            log.warn("Classification failed: {}", ex.getClass().getSimpleName());
            return null;
        }
    }

    /** Signals that the AI service could not serve semantic search; caller may fall back. */
    public static class AiUnavailableException extends RuntimeException {
        public AiUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
