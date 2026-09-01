package com.nia.news.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.nia.config.NiaProperties;
import com.nia.news.NewsProvider;
import com.nia.news.NewsProviderException;
import com.nia.news.NewsQuery;
import com.nia.news.NormalizedArticle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * NewsAPI (newsapi.org) — development only. The free plan forbids production use
 * and delays results ~24h, so this bean is active only under the "dev" profile.
 */
@Component
@Profile("dev")
public class NewsApiProvider implements NewsProvider {

    private static final String BASE = "https://newsapi.org/v2";
    private final WebClient http;
    private final NiaProperties props;

    public NewsApiProvider(@Qualifier("newsWebClient") WebClient http, NiaProperties props) {
        this.http = http;
        this.props = props;
    }

    @Override
    public String name() {
        return "NEWSAPI";
    }

    @Override
    public boolean supports(NewsQuery query) {
        return props.getNews().getNewsapiKey() != null && !props.getNews().getNewsapiKey().isBlank();
    }

    @Override
    public List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException {
        String key = props.getNews().getNewsapiKey();
        UriComponentsBuilder builder;
        if (query.isSearch()) {
            builder = UriComponentsBuilder.fromHttpUrl(BASE + "/everything")
                    .queryParam("q", query.keyword())
                    .queryParam("language", query.language() == null ? "en" : query.language())
                    .queryParam("pageSize", Math.min(query.pageSize(), 20));
        } else {
            builder = UriComponentsBuilder.fromHttpUrl(BASE + "/top-headlines")
                    .queryParam("category", toNewsApiCategory(query.category()))
                    .queryParam("pageSize", Math.min(query.pageSize(), 20));
            String country = "india".equalsIgnoreCase(query.category()) ? "in" : query.country();
            if (country != null) builder.queryParam("country", country);
        }
        builder.queryParam("apiKey", key);

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
                    ProviderUtils.text(a, "urlToImage"),
                    source != null ? ProviderUtils.text(source, "name") : "NewsAPI",
                    ProviderUtils.text(a, "author"),
                    category,
                    query.language() == null ? "en" : query.language(),
                    query.country(),
                    ProviderUtils.parseInstant(ProviderUtils.text(a, "publishedAt")),
                    ProviderUtils.text(a, "content"),
                    "newsapi"));
        }
        return results;
    }

    private JsonNode get(URI uri) throws NewsProviderException {
        try {
            return http.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block();
        } catch (Exception ex) {
            throw new NewsProviderException("NewsAPI request failed", ex);
        }
    }

    private String toNewsApiCategory(String niaCategory) {
        if (niaCategory == null) return "general";
        return switch (niaCategory.toLowerCase()) {
            case "technology" -> "technology";
            case "business" -> "business";
            case "science" -> "science";
            case "sports" -> "sports";
            case "health" -> "health";
            case "entertainment" -> "entertainment";
            default -> "general"; // world, india, politics
        };
    }
}
