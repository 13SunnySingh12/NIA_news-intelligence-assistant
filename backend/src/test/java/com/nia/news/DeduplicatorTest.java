package com.nia.news;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeduplicatorTest {

    private final Deduplicator deduplicator = new Deduplicator();

    private NormalizedArticle article(String title, String url, String content, String provider) {
        return NormalizedArticle.create(title, "desc", url, null, "Source", null,
                "technology", "en", "us", Instant.parse("2026-08-26T10:00:00Z"), content, provider);
    }

    @Test
    void removesDuplicateCanonicalUrls() {
        List<NormalizedArticle> input = List.of(
                article("A story", "https://site.com/a?utm_source=x", null, "gnews"),
                article("A different headline", "https://site.com/a/", null, "newsdata"));

        List<NormalizedArticle> result = deduplicator.dedupe(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).provider()).isEqualTo("gnews"); // higher priority kept
    }

    @Test
    void removesExactTitleDuplicatesOnSameDay() {
        List<NormalizedArticle> input = List.of(
                article("Breaking News Today", "https://a.com/1", null, "gnews"),
                article("breaking news today", "https://b.com/2", null, "newsdata"));

        assertThat(deduplicator.dedupe(input)).hasSize(1);
    }

    @Test
    void removesNearDuplicateTitles() {
        // 9-token title vs. the same 9 tokens plus one extra -> Jaccard 9/10 = 0.9 (>= threshold).
        List<NormalizedArticle> input = List.of(
                article("government announces major new national health care reform plan",
                        "https://a.com/1", null, "gnews"),
                article("government announces major new national health care reform plan today",
                        "https://b.com/2", null, "newsdata"));

        assertThat(deduplicator.dedupe(input)).hasSize(1);
    }

    @Test
    void keepsTitlesBelowSimilarityThreshold() {
        // 7/8 = 0.875 is below 0.9, so these are intentionally NOT treated as duplicates.
        List<NormalizedArticle> input = List.of(
                article("Mars rover finds new evidence of water", "https://a.com/1", null, "gnews"),
                article("Mars rover finds new evidence of water today", "https://b.com/2", null, "newsdata"));

        assertThat(deduplicator.dedupe(input)).hasSize(2);
    }

    @Test
    void keepsDistinctArticles() {
        List<NormalizedArticle> input = List.of(
                article("Story one", "https://a.com/1", null, "gnews"),
                article("Completely unrelated story", "https://b.com/2", null, "newsdata"));

        assertThat(deduplicator.dedupe(input)).hasSize(2);
    }

    @Test
    void prefersDuplicateWithFullContent() {
        List<NormalizedArticle> input = List.of(
                article("Same story", "https://a.com/1", null, "gnews"),
                article("Same story", "https://a.com/1", "full article body", "guardian"));

        List<NormalizedArticle> result = deduplicator.dedupe(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("full article body");
    }
}
