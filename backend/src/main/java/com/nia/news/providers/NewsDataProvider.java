package com.nia.news.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.nia.config.NiaProperties;
import com.nia.news.CategoryMapper;
import com.nia.news.NewsProvider;
import com.nia.news.NewsProviderException;
import com.nia.news.NewsQuery;
import com.nia.news.NormalizedArticle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** NewsData.io — a primary provider with broad country/language coverage. */
@Component
public class NewsDataProvider implements NewsProvider {

    private static final String BASE = "https://newsdata.io/api/1/latest";
    private final WebClient http;
    private final NiaProperties props;
    private final CategoryMapper categoryMapper;

    public NewsDataProvider(@Qualifier("newsWebClient") WebClient http, NiaProperties props,
                            CategoryMapper categoryMapper) {
        this.http = http;
        this.props = props;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public String name() {
        return "NEWSDATA";
    }

    @Override
    public boolean supports(NewsQuery query) {
        return props.getNews().getNewsdataKey() != null && !props.getNews().getNewsdataKey().isBlank();
    }

    @Override
    public List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE)
                .queryParam("apikey", props.getNews().getNewsdataKey())
                .queryParam("language", query.language() == null ? "en" : query.language());

        if (query.isSearch()) {
            builder.queryParam("q", query.keyword());
        } else {
            String ndCategory = toNewsDataCategory(query.category());
            if (ndCategory != null) builder.queryParam("category", ndCategory);
            String country = countryFor(query);
            if (country != null) builder.queryParam("country", country);
        }

        JsonNode body = get(builder.encode().build().toUri());
        List<NormalizedArticle> results = new ArrayList<>();
        JsonNode items = body == null ? null : body.get("results");
        if (items == null || !items.isArray()) {
            return results;
        }
        for (JsonNode a : items) {
            String url = ProviderUtils.text(a, "link");
            if (url == null) continue;
            String category = query.category() != null ? query.category()
                    : categoryMapper.toNia(firstArrayValue(a, "category"));
            results.add(NormalizedArticle.create(
                    ProviderUtils.text(a, "title"),
                    ProviderUtils.text(a, "description"),
                    url,
                    ProviderUtils.text(a, "image_url"),
                    firstNonBlank(ProviderUtils.text(a, "source_id"), "NewsData"),
                    firstArrayValue(a, "creator"),
                    category,
                    firstNonBlank(ProviderUtils.text(a, "language"), query.language() == null ? "en" : query.language()),
                    firstArrayValue(a, "country"),
                    ProviderUtils.parseInstant(ProviderUtils.text(a, "pubDate")),
                    ProviderUtils.text(a, "content"),
                    "newsdata"));
        }
        return results;
    }

    private JsonNode get(URI uri) throws NewsProviderException {
        try {
            return http.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block();
        } catch (Exception ex) {
            throw new NewsProviderException("NewsData request failed", ex);
        }
    }

    private String countryFor(NewsQuery query) {
        if ("india".equalsIgnoreCase(query.category())) return "in";
        return query.country();
    }

    private String toNewsDataCategory(String niaCategory) {
        if (niaCategory == null) return null;
        return switch (niaCategory.toLowerCase()) {
            case "technology" -> "technology";
            case "business" -> "business";
            case "science" -> "science";
            case "sports" -> "sports";
            case "health" -> "health";
            case "entertainment" -> "entertainment";
            case "politics" -> "politics";
            case "world" -> "world";
            case "india" -> "top";
            default -> null;
        };
    }

    private String firstArrayValue(JsonNode node, String field) {
        JsonNode arr = node.get(field);
        if (arr != null && arr.isArray() && !arr.isEmpty()) {
            String v = arr.get(0).asText(null);
            if (v != null && !v.isBlank() && !"null".equalsIgnoreCase(v)) return v.trim();
        }
        return null;
    }

    private String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
