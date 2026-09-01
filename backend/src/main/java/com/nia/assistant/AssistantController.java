package com.nia.assistant;

import com.nia.assistant.dto.AssistantChatRequest;
import com.nia.assistant.dto.AssistantChatResponse;
import com.nia.assistant.dto.SummarizeRequest;
import com.nia.assistant.dto.SummarizeResponse;
import com.nia.auth.UserContext;
import com.nia.common.ApiException;
import com.nia.common.RateLimiter;
import com.nia.config.NiaProperties;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin, rate-limited proxy to the AI service for chat and summaries. Keeps the
 * frontend on a single origin and keeps FastAPI (and its keys) off the internet.
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantClient assistantClient;
    private final UserContext userContext;
    private final RateLimiter rateLimiter;
    private final int assistantPerHour;

    public AssistantController(AssistantClient assistantClient, UserContext userContext,
                              RateLimiter rateLimiter, NiaProperties props) {
        this.assistantClient = assistantClient;
        this.userContext = userContext;
        this.rateLimiter = rateLimiter;
        this.assistantPerHour = props.getRateLimit().getAssistantPerHour();
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest request) {
        String userId = userContext.requireUserId();
        if (!rateLimiter.tryAcquire(userId, "assistant_chat", assistantPerHour)) {
            throw ApiException.rateLimited("You've reached the assistant limit for now — please try again later.");
        }
        return assistantClient.chat(request);
    }

    @PostMapping("/summarize")
    public SummarizeResponse summarize(@Valid @RequestBody SummarizeRequest request) {
        String userId = userContext.requireUserId();
        if (!rateLimiter.tryAcquire(userId, "assistant_summary", assistantPerHour)) {
            throw ApiException.rateLimited("You've reached the summary limit for now — please try again later.");
        }
        String length = (request.length() == null || request.length().isBlank()) ? "short" : request.length();
        return assistantClient.summarize(new SummarizeRequest(request.articleId(), length));
    }
}
