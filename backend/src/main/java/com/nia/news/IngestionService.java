package com.nia.news;

import com.nia.articles.ArticleService;
import com.nia.assistant.AssistantClient;
import com.nia.config.NiaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates one ingestion pass: for each NIA category, query every configured
 * provider, combine, dedupe, persist new articles, and trigger embeddings. Used
 * by both the scheduler and the manual refresh endpoint. Categorization is
 * rule-based (from the query category) and summaries stay on-demand — per NIA's
 * quota-conscious design, articles are not summarized at ingestion.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final NewsAggregatorService aggregator;
    private final ArticleService articleService;
    private final AssistantClient assistantClient;
    private final NiaProperties props;

    /** Guards against overlapping full-ingestion cycles (a slow cycle won't collide with the next). */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public IngestionService(NewsAggregatorService aggregator, ArticleService articleService,
                            AssistantClient assistantClient, NiaProperties props) {
        this.aggregator = aggregator;
        this.articleService = articleService;
        this.assistantClient = assistantClient;
        this.props = props;
    }

    /** Reports coarse progress during a full ingestion pass. */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int percent, String step);
    }

    /** Per-category ingestion outcome, aggregated into the cycle summary. */
    public record CategoryResult(int fetched, int deduped, int newArticles,
                                 Set<String> providersUsed, Set<String> providersFailed) {
    }

    /** Ingest every configured category. Returns the number of new articles saved. */
    public int ingestAll() {
        return ingestAll((percent, step) -> { });
    }

    /**
     * Ingest every configured category, reporting progress and logging a cycle
     * summary. Skipped (returns 0) if another cycle is already running.
     */
    public int ingestAll(ProgressCallback progress) {
        if (!running.compareAndSet(false, true)) {
            log.warn("NIA ingestion skipped — a cycle is already running (overlap prevented)");
            return 0;
        }
        long startMs = System.currentTimeMillis();
        List<String> categories = props.getIngest().getCategories();
        int fetched = 0;
        int saved = 0;
        int embedded = 0;
        Set<String> providersUsed = new TreeSet<>();
        Set<String> providersFailed = new TreeSet<>();
        try {
            for (int i = 0; i < categories.size(); i++) {
                String category = categories.get(i);
                progress.onProgress((int) (i * 100.0 / categories.size()), "Updating " + category);
                CategoryResult result = ingestCategory(category);
                fetched += result.fetched();
                saved += result.newArticles();
                providersUsed.addAll(result.providersUsed());
                providersFailed.addAll(result.providersFailed());
            }
            progress.onProgress(95, "Generating embeddings");
            embedded = embedPending();
            progress.onProgress(100, "Finishing up");
        } finally {
            running.set(false);
        }
        double seconds = (System.currentTimeMillis() - startMs) / 1000.0;
        log.info("NIA ingestion cycle | categories={} providers={} fetched={} duplicatesRemoved={} "
                        + "new={} embedded={} failedProviders={} duration={}s",
                categories.size(), providersUsed, fetched, Math.max(0, fetched - saved), saved, embedded,
                providersFailed.isEmpty() ? "none" : providersFailed, String.format("%.1f", seconds));
        return saved;
    }

    /** Ingest a single category from all providers. Returns a per-category summary. */
    public CategoryResult ingestCategory(String category) {
        NewsQuery query = NewsQuery.topHeadlines(
                category.toLowerCase(),
                null, // providers apply country=in for the "india" category themselves
                props.getIngest().getDefaultLanguage(),
                props.getIngest().getPageSize());

        NewsAggregatorService.AggregationResult agg = aggregator.aggregate(query);
        // Store new articles; embeddings are generated separately (bounded per cycle)
        // so a burst of new articles never trips the embedding provider's rate limit.
        int newCount = articleService.ingest(agg.articles()).size();

        log.debug("Category '{}': fetched={} afterDedup={} new={}",
                category, agg.fetchedRaw(), agg.articles().size(), newCount);
        return new CategoryResult(agg.fetchedRaw(), agg.articles().size(), newCount,
                agg.providersUsed(), agg.providersFailed());
    }

    /** Embed a bounded batch of not-yet-embedded articles (rate-limit-safe). Returns how many. */
    public int embedPending() {
        return assistantClient.embedPending(props.getIngest().getEmbedMaxPerCycle());
    }
}
