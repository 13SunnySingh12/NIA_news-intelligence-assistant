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

/** The Guardian Open Platform — secondary provider with real full-text content. */
@Component
public class GuardianProvider implements NewsProvider {

    private static final String BASE = "https://content.guardianapis.com/search";
    private final WebClient http;
    private final NiaProperties props;

    public GuardianProvider(@Qualifier("newsWebClient") WebClient http, NiaProperties props) {
        this.http = http;
        this.props = props;
    }

    @Override
    public String name() {
        return "GUARDIAN";
    }

    @Override
    public boolean supports(NewsQuery query) {
        return props.getNews().getGuardianKey() != null && !props.getNews().getGuardianKey().isBlank();
    }

    @Override
    public List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE)
                .queryParam("api-key", props.getNews().getGuardianKey())
                .queryParam("show-fields", "trailText,bodyText,thumbnail,byline")
                .queryParam("order-by", "newest")
                .queryParam("page-size", Math.min(query.pageSize(), 50));

        if (query.isSearch()) {
            builder.queryParam("q", query.keyword());
        } else {
            String section = sectionFor(query.category());
            if (section != null) {
                builder.queryParam("section", section);
            } else if (query.category() != null && !"world".equals(query.category())) {
                builder.queryParam("q", query.category());
            }
        }

        JsonNode body = get(builder.encode().build().toUri());
        List<NormalizedArticle> results = new ArrayList<>();
        JsonNode response = body == null ? null : body.get("response");
        JsonNode items = response == null ? null : response.get("results");
        if (items == null || !items.isArray()) {
            return results;
        }
        String category = query.category() != null ? query.category() : "world";
        for (JsonNode a : items) {
            String url = ProviderUtils.text(a, "webUrl");
            if (url == null) continue;
            JsonNode fields = a.get("fields");
            String description = fields != null ? ProviderUtils.text(fields, "trailText") : null;
            String content = fields != null ? ProviderUtils.text(fields, "bodyText") : null;
            String image = fields != null ? ProviderUtils.text(fields, "thumbnail") : null;
            String author = fields != null ? ProviderUtils.text(fields, "byline") : null;
            results.add(NormalizedArticle.create(
                    ProviderUtils.text(a, "webTitle"),
                    description,
                    url,
                    image,
                    "The Guardian",
                    author,
                    category,
                    "en",
                    null,
                    ProviderUtils.parseInstant(ProviderUtils.text(a, "webPublicationDate")),
                    ProviderUtils.truncate(content, 6000),
                    "guardian"));
        }
        return results;
    }

    private JsonNode get(URI uri) throws NewsProviderException {
        try {
            return http.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block();
        } catch (Exception ex) {
            throw new NewsProviderException("Guardian request failed", ex);
        }
    }

    /** NIA category -> Guardian section (null means fall back to a keyword search). */
    private String sectionFor(String niaCategory) {
        if (niaCategory == null) return null;
        return switch (niaCategory.toLowerCase()) {
            case "technology" -> "technology";
            case "business" -> "business";
            case "world" -> "world";
            case "science" -> "science";
            case "sports" -> "sport";
            case "politics" -> "politics";
            default -> null; // health, entertainment, india -> keyword search
        };
    }
}
