package com.nia.news;

import com.nia.news.providers.ProviderUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * A provider-agnostic article. Every {@link NewsProvider} maps its raw response
 * into this shape before aggregation, dedup, and persistence. The {@code provider}
 * field is internal only and is never exposed to the frontend.
 */
public record NormalizedArticle(
        UUID id,
        String title,
        String description,
        String url,
        String imageUrl,
        String source,
        String author,
        String category,
        String language,
        String country,
        Instant publishedAt,
        String content,
        String provider
) {
    /**
     * Convenience factory that assigns a fresh id and normalizes the text fields.
     *
     * Sanitizing here rather than in each provider is deliberate: this is the one
     * point every provider funnels through. Four of the six were passing upstream
     * JSON straight to the database, so an API that embedded markup in its summary
     * (NewsData does, intermittently) put raw &lt;p&gt; and &amp;nbsp; in front of
     * readers. Doing it per-provider means the next provider added has to remember.
     */
    public static NormalizedArticle create(
            String title, String description, String url, String imageUrl,
            String source, String author, String category, String language,
            String country, Instant publishedAt, String content, String provider) {
        return new NormalizedArticle(
                UUID.randomUUID(),
                ProviderUtils.stripHtml(title),
                ProviderUtils.stripHtml(description),
                url, imageUrl, source, author,
                category, language, country, publishedAt,
                ProviderUtils.stripHtml(content),
                provider);
    }
}
