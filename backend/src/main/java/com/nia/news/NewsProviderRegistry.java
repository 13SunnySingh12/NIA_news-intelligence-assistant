package com.nia.news;

import com.nia.config.NiaProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the available {@link NewsProvider} beans and exposes them in the
 * configured priority order (primary, secondary, then fallbacks).
 */
@Component
public class NewsProviderRegistry {

    private final Map<String, NewsProvider> byName = new LinkedHashMap<>();
    private final NiaProperties props;

    public NewsProviderRegistry(List<NewsProvider> providers, NiaProperties props) {
        this.props = props;
        for (NewsProvider provider : providers) {
            byName.put(provider.name().toUpperCase(), provider);
        }
    }

    /** Providers in configured priority order that actually exist as beans. */
    public List<NewsProvider> orderedProviders() {
        List<NewsProvider> ordered = new ArrayList<>();
        for (String name : props.getNews().priorityOrder()) {
            NewsProvider provider = byName.get(name);
            if (provider != null && !ordered.contains(provider)) {
                ordered.add(provider);
            }
        }
        return ordered;
    }
}
