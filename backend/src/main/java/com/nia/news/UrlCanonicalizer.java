package com.nia.news;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Canonicalizes article URLs so duplicates from different providers collapse. */
public final class UrlCanonicalizer {

    private UrlCanonicalizer() {}

    /**
     * Lowercase the host, drop the fragment and common tracking params
     * (utm_*, fbclid, gclid, ...), and remove a trailing slash.
     * Falls back to a trimmed/lowercased string if the URL cannot be parsed.
     */
    public static String canonicalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }
        String trimmed = rawUrl.trim();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase();
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            String query = uri.getQuery();
            String cleanedQuery = "";
            if (query != null && !query.isBlank()) {
                cleanedQuery = Arrays.stream(query.split("&"))
                        .filter(param -> {
                            String key = param.split("=", 2)[0].toLowerCase();
                            return !key.startsWith("utm_")
                                    && !key.equals("fbclid")
                                    && !key.equals("gclid")
                                    && !key.equals("mc_cid")
                                    && !key.equals("mc_eid")
                                    && !key.equals("igshid")
                                    && !key.equals("ref");
                        })
                        .collect(Collectors.joining("&"));
            }

            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host).append(path);
            if (!cleanedQuery.isBlank()) {
                sb.append("?").append(cleanedQuery);
            }
            return sb.toString();
        } catch (Exception ex) {
            return trimmed.toLowerCase();
        }
    }
}
