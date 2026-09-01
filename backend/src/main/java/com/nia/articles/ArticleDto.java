package com.nia.articles;

import com.nia.articles.model.Article;

import java.time.Instant;
import java.util.UUID;

/**
 * The article shape the frontend receives. Deliberately excludes internal fields
 * (provider, canonical URL, embedding). {@code content} is only included on the
 * detail view to keep list payloads small.
 */
public record ArticleDto(
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
        long readCount,
        boolean bookmarked
) {
    public static ArticleDto of(Article a, boolean includeContent, boolean bookmarked) {
        return new ArticleDto(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getUrl(),
                a.getImageUrl(),
                a.getSource(),
                a.getAuthor(),
                a.getCategory(),
                a.getLanguage(),
                a.getCountry(),
                a.getPublishedAt(),
                includeContent ? a.getContent() : null,
                a.getReadCount(),
                bookmarked);
    }
}
