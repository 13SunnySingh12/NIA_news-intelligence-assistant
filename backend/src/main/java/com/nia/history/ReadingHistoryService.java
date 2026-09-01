package com.nia.history;

import com.nia.articles.ArticleRepository;
import com.nia.common.ApiException;
import com.nia.history.model.ReadingHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Records a read event: bumps the article's global read counter (powers trending)
 * and appends a per-user history row (powers personalization).
 */
@Service
public class ReadingHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ReadingHistoryService.class);

    private final ArticleRepository articleRepository;
    private final ReadingHistoryRepository readingHistoryRepository;

    public ReadingHistoryService(ArticleRepository articleRepository,
                                 ReadingHistoryRepository readingHistoryRepository) {
        this.articleRepository = articleRepository;
        this.readingHistoryRepository = readingHistoryRepository;
    }

    /**
     * The history row is what matters (it powers personalization), so it is written
     * first in its own transaction. The global read counter only powers trending, so
     * it is best-effort: while an ingestion cycle is writing to `articles`, that row
     * can be locked, and a slow counter update must never fail the user's request.
     */
    public void recordRead(String userId, UUID articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw ApiException.notFound("That article couldn't be found.");
        }
        readingHistoryRepository.save(new ReadingHistory(UUID.fromString(userId), articleId));
        try {
            articleRepository.incrementReadCount(articleId);
        } catch (Exception ex) {
            log.debug("Skipped read-count bump for {}: {}", articleId, ex.getClass().getSimpleName());
        }
    }
}
