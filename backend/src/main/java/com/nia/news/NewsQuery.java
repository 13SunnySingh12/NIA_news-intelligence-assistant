package com.nia.news;

/**
 * A request for news, independent of any specific provider.
 * Either a category/country headline query or a keyword search.
 */
public record NewsQuery(
        String category,   // NIA category, or null for a plain search
        String country,    // ISO 3166-1 alpha-2, or null
        String language,   // ISO 639-1
        String keyword,    // search term, or null for top headlines
        int pageSize
) {
    public static NewsQuery topHeadlines(String category, String country, String language, int pageSize) {
        return new NewsQuery(category, country, language, null, pageSize);
    }

    public static NewsQuery search(String keyword, String language, int pageSize) {
        return new NewsQuery(null, null, language, keyword, pageSize);
    }

    public boolean isSearch() {
        return keyword != null && !keyword.isBlank();
    }
}
