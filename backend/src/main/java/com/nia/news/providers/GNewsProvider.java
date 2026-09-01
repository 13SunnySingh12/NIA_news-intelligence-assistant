package com.nia.news.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.nia.config.NiaProperties;
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

/** GNews (gnews.io) — a primary provider: broad real-time headlines and search. */
@Component
public class GNewsProvider implements NewsProvider {

    private static final String BASE = "https://gnews.io/api/v4";
    private final WebClient http;
    private final NiaProperties props;

    public GNewsProvider(@Qualifier("newsWebClient") WebClient http, NiaProperties props) {
        this.http = http;
        this.props = props;
    }

    @Override
    public String name() {
        return "GNEWS";
    }

    @Override
    public boolean supports(NewsQuery query) {
        return props.getNews().getGnewsKey() != null && !props.getNews().getGnewsKey().isBlank();
    }

    @Override
    public List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException {
        String key = props.getNews().getGnewsKey();
        UriComponentsBuilder builder;
        if (query.isSearch()) {
            builder = UriComponentsBuilder.fromHttpUrl(BASE + "/search")
                    .queryParam("q", query.keyword());
        } else {
            builder = UriComponentsBuilder.fromHttpUrl(BASE + "/top-headlines")
                    .queryParam("category", toGNewsCategory(query.category()));
            String country = countryFor(query);
            if (country != null) builder.queryParam("country", country);
        }
        builder.queryParam("lang", query.language() == null ? "en" : query.language())
                .queryParam("max", Math.min(query.pageSize(), 10))
                .queryParam("apikey", key);

        JsonNode body = get(builder.encode().build().toUri());
        List<NormalizedArticle> results = new ArrayList<>();
        JsonNode articles = body == null ? null : body.get("articles");
        if (articles == null || !articles.isArray()) {
            return results;
        }
        String category = query.category() != null ? query.category() : "world";
        for (JsonNode a : articles) {
            String url = ProviderUtils.text(a, "url");
            if (url == null) continue;
            JsonNode source = a.get("source");
            results.add(NormalizedArticle.create(
                    ProviderUtils.text(a, "title"),
                    ProviderUtils.text(a, "description"),
                    url,
                    ProviderUtils.text(a, "image"),
                    source != null ? ProviderUtils.text(source, "name") : "GNews",
                    null,
                    category,
                    query.language() == null ? "en" : query.language(),
                    countryFor(query),
                    ProviderUtils.parseInstant(ProviderUtils.text(a, "publishedAt")),
                    ProviderUtils.text(a, "content"),
                    "gnews"));
        }
        return results;
    }

    private JsonNode get(URI uri) throws NewsProviderException {
        try {
            return http.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block();
        } catch (Exception ex) {
            throw new NewsProviderException("GNews request failed", ex);
        }
    }

    private String countryFor(NewsQuery query) {
        if ("india".equalsIgnoreCase(query.category())) return "in";
        return query.country();
    }

    /** NIA category -> GNews category vocabulary. */
    private String toGNewsCategory(String niaCategory) {
        if (niaCategory == null) return "general";
        return switch (niaCategory.toLowerCase()) {
            case "technology" -> "technology";
            case "business" -> "business";
            case "science" -> "science";
            case "sports" -> "sports";
            case "health" -> "health";
            case "entertainment" -> "entertainment";
            case "politics" -> "nation";
            case "india" -> "nation";
            default -> "general"; // world and anything else
        };
    }
}
