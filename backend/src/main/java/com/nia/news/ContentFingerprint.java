package com.nia.news;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * A source-independent fingerprint for an article: SHA-256 of its normalized
 * title plus its UTC publication day. Because it ignores the provider and URL,
 * the same story reported by different providers on the same day hashes to the
 * same value — which lets us drop cross-provider and cross-cycle duplicates that
 * a canonical-URL match alone would miss. The day is included so a genuinely new
 * article that happens to reuse an old headline is not treated as a duplicate.
 */
public final class ContentFingerprint {

    private ContentFingerprint() {}

    public static String of(String title, Instant publishedAt) {
        String normalizedTitle = title == null ? ""
                : title.toLowerCase()
                       .replaceAll("[^a-z0-9\\s]", " ")
                       .replaceAll("\\s+", " ")
                       .trim();
        String day = publishedAt == null ? ""
                : LocalDate.ofInstant(publishedAt, ZoneOffset.UTC).toString();
        return sha256(normalizedTitle + "|" + day);
    }

    private static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception ex) {
            // SHA-256 is always available; fall back to a plain key if it somehow isn't.
            return Integer.toHexString(input.hashCode());
        }
    }
}
