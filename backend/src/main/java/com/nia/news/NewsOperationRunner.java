package com.nia.news;

import com.nia.config.AsyncConfig;
import com.nia.operations.OperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Runs a news-refresh operation on a background thread so it is independent of
 * the browser: it keeps going when the user minimizes, switches tabs, navigates
 * away, or closes the page. It updates the operation record as it progresses.
 */
@Component
public class NewsOperationRunner {

    private static final Logger log = LoggerFactory.getLogger(NewsOperationRunner.class);

    private final OperationService operationService;
    private final IngestionService ingestionService;

    public NewsOperationRunner(OperationService operationService, IngestionService ingestionService) {
        this.operationService = operationService;
        this.ingestionService = ingestionService;
    }

    @Async(AsyncConfig.OPERATION_EXECUTOR)
    public void run(UUID operationId, String category) {
        try {
            operationService.markRunning(operationId, "Starting news update", 5);
            int saved;
            if (category != null && !category.isBlank()) {
                operationService.updateProgress(operationId, 40, "Updating " + category);
                saved = ingestionService.ingestCategory(category).newArticles();
                operationService.updateProgress(operationId, 80, "Generating embeddings");
                ingestionService.embedPending();
            } else {
                // Keep provider progress within a 5–95 band; 100 is reserved for completion.
                saved = ingestionService.ingestAll((percent, step) ->
                        operationService.updateProgress(operationId, 5 + (int) Math.round(percent * 0.9), step));
            }
            operationService.markCompleted(operationId, Map.of("newArticles", saved));
        } catch (Exception ex) {
            log.error("News refresh operation {} failed", operationId, ex);
            operationService.markFailed(operationId, "News update failed. Please try again shortly.");
        }
    }
}
