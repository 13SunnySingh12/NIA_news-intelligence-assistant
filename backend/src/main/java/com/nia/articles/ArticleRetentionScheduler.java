package com.nia.articles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Keeps the article table bounded. Ingestion runs every few minutes and never
 * deletes anything, so without this the database grows without limit and
 * eventually exceeds the hosting plan's storage.
 *
 * Only stale articles are removed, and never one a user still refers to
 * (bookmarked or read). NIA's feed only ranks the last 48h and trending only
 * looks at 24h, so a retention window of a few days loses nothing the UI uses.
 */
@Component
public class ArticleRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ArticleRetentionScheduler.class);

    private final ArticleRepository articleRepository;
    private final int retentionDays;
    private final boolean enabled;

    public ArticleRetentionScheduler(ArticleRepository articleRepository,
                                     @Value("${nia.retention.article-days:7}") int retentionDays,
                                     @Value("${nia.retention.enabled:true}") boolean enabled) {
        this.articleRepository = articleRepository;
        this.retentionDays = retentionDays;
        this.enabled = enabled;
    }

    /** Runs a few minutes past every hour, offset from the ingestion cron. */
    @Scheduled(cron = "${nia.retention.cron:0 5 * * * *}")
    public void purgeOldArticles() {
        if (!enabled || retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        try {
            int removed = articleRepository.deleteOlderThan(cutoff);
            if (removed > 0) {
                log.info("NIA retention | removed={} articles older than {} days", removed, retentionDays);
            }
        } catch (Exception ex) {
            // Never let cleanup take the app down; it retries next hour.
            log.warn("Article retention pass failed: {}", ex.getMessage());
        }
    }
}
