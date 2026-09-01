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

/** Currents API — a fallback provider used when the primaries are throttled. */
@Component
public class CurrentsProvider implements NewsProvider {

    private static final String LATEST = "https://api.currentsapi.services/v1/latest-news";
    private static final String SEARCH = "https://api.currentsapi.services/v1/search";
    private final WebClient http;
    private final NiaProperties props;
    private final CategoryMapper categoryMapper;

    public CurrentsProvider(@Qualifier("newsWebClient") WebClient http, NiaProperties props,
                            CategoryMapper categoryMapper) {
        this.http = http;
        this.props = props;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public String name() {
        return "CURRENTS";
    }

    @Override
    public boolean supports(NewsQuery query) {
        return props.getNews().getCurrentsKey() != null && !props.getNews().getCurrentsKey().isBlank();
    }

    @Override
    public List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException {
        String lang = query.language() == null ? "en" : query.language();
        UriComponentsBuilder builder;
        if (query.isSearch()) {
            builder = UriComponentsBuilder.fromHttpUrl(SEARCH)
                    .queryParam("keywords", query.keyword())
                    .queryParam("language", lang);
        } else {
            builder = UriComponentsBuilder.fromHttpUrl(LATEST)
                    .queryParam("language", lang);
            String currentsCategory = toCurrentsCategory(query.category());
            if (currentsCategory != null) builder.queryParam("category", currentsCategory);
        }
        builder.queryParam("apiKey", props.getNews().getCurrentsKey());

        JsonNode body = get(builder.encode().build().toUri());
        List<NormalizedArticle> results = new ArrayList<>();
        JsonNode news = body == null ? null : body.get("news");
        if (news == null || !news.isArray()) {
            return results;
        }
        for (JsonNode a : news) {
            String url = ProviderUtils.text(a, "url");
            if (url == null) continue;
            String category = query.category() != null ? query.category()
                    : categoryMapper.toNia(firstArrayValue(a, "category"));
            results.add(NormalizedArticle.create(
                    ProviderUtils.text(a, "title"),
                    ProviderUtils.text(a, "description"),
                    url,
                    ProviderUtils.text(a, "image"),
                    "Currents",
                    ProviderUtils.text(a, "author"),
                    category,
                    lang,
                    countryFor(query),
                    ProviderUtils.parseInstant(ProviderUtils.text(a, "published")),
                    null,
                    "currents"));
        }
        return results;
    }

    private JsonNode get(URI uri) throws NewsProviderException {
        try {
            return http.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block();
        } catch (Exception ex) {
            throw new NewsProviderException("Currents request failed", ex);
        }
    }

    private String countryFor(NewsQuery query) {
        if ("india".equalsIgnoreCase(query.category())) return "in";
        return query.country();
    }

    private String toCurrentsCategory(String niaCategory) {
        if (niaCategory == null || "india".equalsIgnoreCase(niaCategory)) return null;
        return switch (niaCategory.toLowerCase()) {
            case "technology" -> "technology";
            case "business" -> "business";
            case "science" -> "science";
            case "sports" -> "sports";
            case "health" -> "health";
            case "entertainment" -> "entertainment";
            case "politics" -> "politics";
            case "world" -> "world";
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
}
