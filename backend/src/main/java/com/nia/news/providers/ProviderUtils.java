package com.nia.news.providers;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small parsing helpers shared by the news providers. External data is messy. */
public final class ProviderUtils {

    private ProviderUtils() {}

    /** Returns a trimmed field value, or null if missing/blank/JSON-null. */
    static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String s = value.asText();
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s) || "None".equalsIgnoreCase(s)) {
            return null;
        }
        return s.trim();
    }

    /** Parses several common date formats; falls back to now() so ingestion never fails on a bad date. */
    static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return Instant.now();
        String s = raw.trim();
        try {
            return Instant.parse(s);
        } catch (Exception ignored) { /* try next */ }
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (Exception ignored) { /* try next */ }
        try {
            // e.g. "2026-08-26 10:15:00" (NewsData / Currents style)
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) { /* try next */ }
        try {
            return OffsetDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception ignored) { /* give up */ }
        return Instant.now();
    }

    /**
     * Named HTML entities that actually turn up in news feeds. Numeric entities are
     * handled separately, so only the common named ones are needed here.
     */
    private static final Map<String, String> ENTITIES = Map.ofEntries(
            Map.entry("nbsp", " "), Map.entry("amp", "&"), Map.entry("lt", "<"),
            Map.entry("gt", ">"), Map.entry("quot", "\""), Map.entry("apos", "'"),
            Map.entry("rsquo", "’"), Map.entry("lsquo", "‘"),
            Map.entry("ldquo", "“"), Map.entry("rdquo", "”"),
            Map.entry("hellip", "…"), Map.entry("mdash", "—"),
            Map.entry("ndash", "–"), Map.entry("middot", "·"),
            Map.entry("bull", "•"), Map.entry("laquo", "«"),
            Map.entry("raquo", "»"), Map.entry("deg", "°"),
            Map.entry("eacute", "é"), Map.entry("egrave", "è"),
            Map.entry("agrave", "à"), Map.entry("ccedil", "ç"),
            Map.entry("uuml", "ü"), Map.entry("ouml", "ö"),
            Map.entry("auml", "ä"), Map.entry("szlig", "ß"),
            Map.entry("pound", "£"), Map.entry("euro", "€"),
            Map.entry("yen", "¥"), Map.entry("cent", "¢"),
            Map.entry("copy", "©"), Map.entry("reg", "®"),
            Map.entry("trade", "™"), Map.entry("times", "×"));

    private static final Pattern ENTITY = Pattern.compile("&(#[xX]?[0-9a-fA-F]+|[a-zA-Z]+);");

    /**
     * Strips markup and decodes HTML entities.
     *
     * Feeds deliver HTML, not plain text: a Google News RSS description arrives as
     * {@code <a ...>Headline</a>&nbsp;&nbsp;<font>Source</font>}. Removing tags alone
     * left the entities behind, so readers saw a literal "&nbsp;&nbsp;" in the card.
     * Decoding runs AFTER tags are removed, so a decoded '<' can never reintroduce
     * markup.
     */
    public static String stripHtml(String s) {
        if (s == null) return null;
        String cleaned = s.replaceAll("<[^>]+>", " ");
        // A field truncated to a length limit can end mid-tag ("...text<p"), which
        // has no closing '>' for the rule above to match. Require a letter or '/'
        // right after the '<' so this only ever removes a real tag start - a bare
        // '<' in prose ("5 < 10") must not swallow the rest of the sentence.
        cleaned = cleaned.replaceAll("<[a-zA-Z/][^>]*$", " ");
        cleaned = decodeEntities(cleaned);
        // U+00A0 (from &nbsp;) reads as a space but is not matched by \s here,
        // so fold it to a plain space before collapsing runs of whitespace.
        cleaned = cleaned.replace(' ', ' ').replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    /** Decodes named and numeric HTML entities; anything unrecognized is left as-is. */
    static String decodeEntities(String s) {
        if (s == null || s.indexOf('&') < 0) return s;
        Matcher m = ENTITY.matcher(s);
        StringBuilder out = new StringBuilder(s.length());
        while (m.find()) {
            String token = m.group(1);
            String replacement = null;
            if (token.charAt(0) == '#') {
                try {
                    boolean hex = token.charAt(1) == 'x' || token.charAt(1) == 'X';
                    int code = Integer.parseInt(token.substring(hex ? 2 : 1), hex ? 16 : 10);
                    if (Character.isValidCodePoint(code) && code != 0) {
                        replacement = new String(Character.toChars(code));
                    }
                } catch (RuntimeException ignored) {
                    // Malformed numeric entity - leave the original text alone.
                }
            } else {
                replacement = ENTITIES.get(token.toLowerCase(Locale.ROOT));
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement != null ? replacement : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
