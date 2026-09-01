package com.nia.news;

import com.nia.config.NiaProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the multi-provider aggregation: combine all providers, dedupe, and survive failures. */
class NewsAggregatorServiceTest {

    private NormalizedArticle sample(String title, String url, String provider) {
        return NormalizedArticle.create(title, "desc", url, null, "Source", null,
                "world", "en", "us", Instant.now(), null, provider);
    }

    private NiaProperties propsWithOrder(String primary, String secondary) {
        NiaProperties props = new NiaProperties();
        props.getNews().setPrimary(primary);
        props.getNews().setSecondary(secondary);
        props.getNews().setFallback(List.of());
        return props;
    }

    private NewsAggregatorService aggregatorFor(NewsProvider... providers) {
        NewsProviderRegistry registry = new NewsProviderRegistry(
                List.of(providers), propsWithOrder("PRIMARY", "SECONDARY"));
        return new NewsAggregatorService(registry, new Deduplicator());
    }

    @Test
    void queriesAllProvidersAndCombinesResults() {
        NewsProvider a = stub("PRIMARY", q -> List.of(sample("From A", "https://a.com/1", "a")));
        NewsProvider b = stub("SECONDARY", q -> List.of(sample("From B", "https://b.com/2", "b")));

        var result = aggregatorFor(a, b).aggregate(NewsQuery.topHeadlines("world", null, "en", 10));

        // Both providers contributed in the same cycle (no early stop).
        assertThat(result.articles()).hasSize(2);
        assertThat(result.fetchedRaw()).isEqualTo(2);
        assertThat(result.providersUsed()).containsExactlyInAnyOrder("PRIMARY", "SECONDARY");
        assertThat(result.providersFailed()).isEmpty();
    }

    @Test
    void continuesWhenOneProviderFails() {
        NewsProvider failing = stub("PRIMARY", q -> { throw new NewsProviderException("boom"); });
        NewsProvider working = stub("SECONDARY", q -> List.of(sample("From B", "https://b.com/1", "b")));

        var result = aggregatorFor(failing, working).aggregate(NewsQuery.topHeadlines("world", null, "en", 10));

        assertThat(result.articles()).hasSize(1);
        assertThat(result.articles().get(0).provider()).isEqualTo("b");
        assertThat(result.providersFailed()).containsExactly("PRIMARY");
        assertThat(result.providersUsed()).containsExactly("SECONDARY");
    }

    @Test
    void dedupesTheSameStoryAcrossProviders() {
        // Same canonical URL reported by two providers -> stored once.
        NewsProvider a = stub("PRIMARY", q -> List.of(sample("Shared story", "https://x.com/story", "a")));
        NewsProvider b = stub("SECONDARY", q -> List.of(sample("Shared story", "https://x.com/story", "b")));

        var result = aggregatorFor(a, b).aggregate(NewsQuery.topHeadlines("world", null, "en", 10));

        assertThat(result.fetchedRaw()).isEqualTo(2);   // both fetched it
        assertThat(result.articles()).hasSize(1);        // deduped to one
    }

    private interface Fetcher {
        List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException;
    }

    private NewsProvider stub(String name, Fetcher fetcher) {
        return new NewsProvider() {
            @Override public String name() { return name; }
            @Override public boolean supports(NewsQuery query) { return true; }
            @Override public List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException {
                return fetcher.fetch(query);
            }
        };
    }
}
