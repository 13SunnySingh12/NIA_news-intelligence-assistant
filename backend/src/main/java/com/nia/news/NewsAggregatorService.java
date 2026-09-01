package com.nia.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fetches from EVERY configured, non-exhausted provider in the same cycle, merges
 * their results, and removes duplicates — giving broader coverage than a single
 * provider. Tracks a per-provider daily request count so a provider near its
 * documented free-tier limit is skipped instead of paying for a 429. One
 * provider's failure never stops the others.
 *
 * Providers are queried CONCURRENTLY: they are independent network calls, so a
 * cycle now costs roughly the slowest provider rather than the sum of them all.
 * A slow or failing provider therefore cannot hold up the rest of the feed.
 */
@Service
public class NewsAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(NewsAggregatorService.class);

    /** Conservative soft daily caps (a fraction below documented free limits). */
    private static final Map<String, Integer> DAILY_CAPS = Map.of(
            "GNEWS", 90,
            "NEWSDATA", 180,
            "GUARDIAN", 4500,
            "CURRENTS", 550,
            "NEWSAPI", 90);

    /** One slot per provider: they are IO-bound, so a small fixed pool is plenty. */
    private static final int MAX_PARALLEL_PROVIDERS = 6;
    /** A provider that exceeds this simply loses its slot for the cycle. */
    private static final long PROVIDER_TIMEOUT_SECONDS = 20;

    private final NewsProviderRegistry registry;
    private final Deduplicator deduplicator;
    private final Map<String, DailyCounter> counters = new ConcurrentHashMap<>();
    private final ExecutorService fetchPool =
            Executors.newFixedThreadPool(MAX_PARALLEL_PROVIDERS, runnable -> {
                Thread thread = new Thread(runnable, "nia-provider");
                thread.setDaemon(true);
                return thread;
            });

    public NewsAggregatorService(NewsProviderRegistry registry, Deduplicator deduplicator) {
        this.registry = registry;
        this.deduplicator = deduplicator;
    }

    /** Deduplicated articles for a query plus which providers contributed or failed. */
    public record AggregationResult(
            List<NormalizedArticle> articles,
            int fetchedRaw,
            Set<String> providersUsed,
            Set<String> providersFailed) {
    }

    /**
     * Query every supported, non-exhausted provider for this cycle, combine their
     * results, and deduplicate. A provider that errors or is over its daily cap is
     * skipped without affecting the others.
     */
    public AggregationResult aggregate(NewsQuery query) {
        Set<String> used = java.util.Collections.synchronizedSet(new LinkedHashSet<>());
        Set<String> failed = java.util.Collections.synchronizedSet(new LinkedHashSet<>());

        // Start every eligible provider at once; each one resolves independently.
        List<CompletableFuture<List<NormalizedArticle>>> futures = new ArrayList<>();
        for (NewsProvider provider : registry.orderedProviders()) {
            if (!provider.supports(query)) {
                continue;
            }
            if (isExhausted(provider.name())) {
                log.debug("Skipping {} — near daily limit", provider.name());
                continue;
            }
            recordRequest(provider.name());
            futures.add(fetchAsync(provider, query, used, failed));
        }

        // Preserve registry order in the merged list so dedup keeps the
        // highest-priority provider's copy, exactly as before.
        List<NormalizedArticle> merged = new ArrayList<>();
        for (CompletableFuture<List<NormalizedArticle>> future : futures) {
            merged.addAll(future.join());   // never throws: failures return an empty list
        }

        int fetchedRaw = merged.size();
        List<NormalizedArticle> deduped = deduplicator.dedupe(merged);
        return new AggregationResult(deduped, fetchedRaw, used, failed);
    }

    /** Fetch one provider off the pool. Any failure or timeout yields an empty list. */
    private CompletableFuture<List<NormalizedArticle>> fetchAsync(
            NewsProvider provider, NewsQuery query, Set<String> used, Set<String> failed) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        List<NormalizedArticle> fetched = provider.fetch(query);
                        used.add(provider.name());
                        return fetched == null ? List.<NormalizedArticle>of() : fetched;
                    } catch (Exception ex) {
                        failed.add(provider.name());
                        // Never log keys; only the provider name and its message.
                        log.warn("Provider {} failed: {}", provider.name(), ex.getMessage());
                        return List.<NormalizedArticle>of();
                    }
                }, fetchPool)
                .orTimeout(PROVIDER_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    failed.add(provider.name());
                    log.warn("Provider {} timed out after {}s", provider.name(), PROVIDER_TIMEOUT_SECONDS);
                    return List.of();
                });
    }

    private boolean isExhausted(String provider) {
        Integer cap = DAILY_CAPS.get(provider.toUpperCase());
        if (cap == null) {
            return false; // e.g. Google News RSS — no documented cap
        }
        DailyCounter counter = counters.get(provider.toUpperCase());
        return counter != null && counter.currentCount() >= cap;
    }

    private void recordRequest(String provider) {
        counters.computeIfAbsent(provider.toUpperCase(), k -> new DailyCounter()).increment();
    }

    /** Per-provider counter that resets each UTC day. */
    private static final class DailyCounter {
        private volatile long dayEpoch = today();
        private final AtomicInteger count = new AtomicInteger(0);

        synchronized void increment() {
            rolloverIfNeeded();
            count.incrementAndGet();
        }

        synchronized int currentCount() {
            rolloverIfNeeded();
            return count.get();
        }

        private void rolloverIfNeeded() {
            long now = today();
            if (now != dayEpoch) {
                dayEpoch = now;
                count.set(0);
            }
        }

        private static long today() {
            return Instant.now().getEpochSecond() / 86_400;
        }
    }
}
