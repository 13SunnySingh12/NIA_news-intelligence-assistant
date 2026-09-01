package com.nia.news;

import java.util.List;

/**
 * A single news source. Adding a provider means implementing this interface,
 * registering the bean, and adding its name to the priority list — no changes
 * to controllers, the frontend, or the database.
 */
public interface NewsProvider {

    /** Registry key, e.g. "GNEWS", "GUARDIAN". Matches the provider order config. */
    String name();

    /** Whether this provider is configured (has a key if required) and can serve the query. */
    boolean supports(NewsQuery query);

    /** Fetch and normalize articles. Implementations must not throw on empty results. */
    List<NormalizedArticle> fetch(NewsQuery query) throws NewsProviderException;
}
