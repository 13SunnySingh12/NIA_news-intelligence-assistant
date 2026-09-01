package com.nia.news;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Maps arbitrary provider category strings onto NIA's fixed category set.
 * The canonical list must stay in sync with NIA_INGEST_CATEGORIES and
 * the frontend category list.
 */
@Component
public class CategoryMapper {

    public static final List<String> NIA_CATEGORIES = List.of(
            "technology", "business", "world", "india", "science",
            "sports", "health", "entertainment", "politics");

    private static final Set<String> VALID = Set.copyOf(NIA_CATEGORIES);

    /** Tokens that legitimately mean "world" (everything else falling to world is a guess). */
    private static final Set<String> KNOWN_WORLD_TOKENS = Set.of(
            "world", "general", "international", "top", "breaking-news", "news");

    /** Best-effort mapping of a provider's category token to a NIA category. */
    public String toNia(String raw) {
        if (raw == null || raw.isBlank()) {
            return "world";
        }
        String c = raw.trim().toLowerCase();
        return switch (c) {
            case "technology", "tech" -> "technology";
            case "business", "finance", "economy", "money" -> "business";
            case "science", "environment" -> "science";
            case "sports", "sport", "football" -> "sports";
            case "health" -> "health";
            case "entertainment", "showbiz", "lifestyle", "culture" -> "entertainment";
            case "politics", "nation", "national" -> "politics";
            case "world", "general", "international", "top", "breaking-news", "news" -> "world";
            default -> VALID.contains(c) ? c : "world";
        };
    }

    /**
     * Confident mapping only: returns {@code null} when the provider's token isn't
     * recognised, so the caller can ask the AI service instead of silently
     * bucketing the article into "world".
     */
    public String toNiaOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String mapped = toNia(raw);
        // toNia() defaults unknown tokens to "world"; treat that as "not mapped"
        // unless the provider genuinely said world/general/international.
        if ("world".equals(mapped) && !KNOWN_WORLD_TOKENS.contains(raw.trim().toLowerCase())) {
            return null;
        }
        return mapped;
    }

    public boolean isValid(String category) {
        return category != null && VALID.contains(category.trim().toLowerCase());
    }
}
