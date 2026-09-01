package com.nia.news.providers;

import com.nia.news.NewsProvider;
import com.nia.news.NewsProviderException;
import com.nia.news.NewsQuery;
import com.nia.news.NormalizedArticle;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Google News RSS — free, keyless, last-resort supplemental source.
 * Provides titles and links only: there is no full text and no real summary,
 * so both content and description stay null.
 */
@Component
public class GoogleNewsRssProvider implements NewsProvider {

    private static final String SEARCH = "https://news.google.com/rss/search";
    private final WebClient http;

    public GoogleNewsRssProvider(@Qualifier("newsWebClient") WebClient http) {
        this.http = http;
    }

    @Override
    public String name() {
        return "GOOGLE_NEWS_RSS";
    }

    @Override
    public boolean supports(NewsQuery query) {
        return true; // no key required
    }

    @Override
    public List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException {
        boolean india = "india".equalsIgnoreCase(query.category());
        String q = query.isSearch() ? query.keyword()
                : (query.category() == null ? "top stories" : query.category());

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(SEARCH)
                .queryParam("q", q)
                .queryParam("hl", india ? "en-IN" : "en-US")
                .queryParam("gl", india ? "IN" : "US")
                .queryParam("ceid", india ? "IN:en" : "US:en");

        String xml = getXml(builder.encode().build().toUri());
        List<NormalizedArticle> results = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return results;
        }
        String category = query.category() != null ? query.category() : "world";
        try {
            SyndFeed feed = new SyndFeedInput().build(new StringReader(xml));
            for (SyndEntry entry : feed.getEntries()) {
                String link = entry.getLink();
                if (link == null || link.isBlank()) continue;
                String rawTitle = entry.getTitle();
                results.add(NormalizedArticle.create(
                        cleanTitle(rawTitle),
                        // No description: this feed has no summary field. Its
                        // <description> is the headline repeated plus the publisher
                        // name (checked against every stored row: the trailing text
                        // was the article's own source, every time, up to 61 chars
                        // long), or a list of related headlines. Storing it made
                        // every card print the same sentence twice.
                        null,
                        link,
                        null,
                        sourceFromTitle(rawTitle),
                        null,
                        category,
                        "en",
                        india ? "in" : query.country(),
                        entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant() : Instant.now(),
                        null,
                        "google_news_rss"));
            }
        } catch (Exception ex) {
            throw new NewsProviderException("Google News RSS parse failed", ex);
        }
        return results;
    }

    private String getXml(URI uri) throws NewsProviderException {
        try {
            return http.get().uri(uri).retrieve().bodyToMono(String.class).block();
        } catch (Exception ex) {
            throw new NewsProviderException("Google News RSS request failed", ex);
        }
    }

    /** Google News titles look like "Headline - Source"; keep the headline. */
    private String cleanTitle(String title) {
        if (title == null) return null;
        int idx = title.lastIndexOf(" - ");
        return idx > 0 ? title.substring(0, idx).trim() : title.trim();
    }

    private String sourceFromTitle(String title) {
        if (title == null) return "Google News";
        int idx = title.lastIndexOf(" - ");
        return idx > 0 && idx < title.length() - 3 ? title.substring(idx + 3).trim() : "Google News";
    }
}
