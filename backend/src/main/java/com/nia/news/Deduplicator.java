package com.nia.news;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes duplicate articles after normalization and before persistence.
 * The input list is expected in provider-priority order (higher priority first),
 * so "keep the first one seen" naturally prefers the higher-priority provider.
 *
 * Rules (in order): canonical URL match, exact normalized-title match on the same
 * UTC day, then near-duplicate title match (Jaccard >= 0.9) on the same day and
 * category. No AI, no LSH — deliberately simple.
 */
@Component
public class Deduplicator {

    private static final double NEAR_DUPLICATE_THRESHOLD = 0.9;

    public List<NormalizedArticle> dedupe(List<NormalizedArticle> articles) {
        List<NormalizedArticle> kept = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        Set<String> seenTitleDay = new HashSet<>();

        for (NormalizedArticle candidate : articles) {
            String canonical = UrlCanonicalizer.canonicalize(candidate.url());
            String normalizedTitle = normalizeTitle(candidate.title());
            String day = dayKey(candidate);
            String titleDayKey = normalizedTitle + "|" + day;

            if (!canonical.isBlank() && seenUrls.contains(canonical)) {
                maybeUpgrade(kept, candidate);
                continue;
            }
            if (seenTitleDay.contains(titleDayKey)) {
                maybeUpgrade(kept, candidate);
                continue;
            }
            if (isNearDuplicate(kept, candidate, normalizedTitle, day)) {
                continue;
            }

            kept.add(candidate);
            if (!canonical.isBlank()) {
                seenUrls.add(canonical);
            }
            seenTitleDay.add(titleDayKey);
        }
        return kept;
    }

    /** Tie-break: if a duplicate carries full content and the kept copy doesn't, swap it in. */
    private void maybeUpgrade(List<NormalizedArticle> kept, NormalizedArticle candidate) {
        if (candidate.content() == null || candidate.content().isBlank()) {
            return;
        }
        String candidateUrl = UrlCanonicalizer.canonicalize(candidate.url());
        for (int i = 0; i < kept.size(); i++) {
            NormalizedArticle existing = kept.get(i);
            boolean sameUrl = UrlCanonicalizer.canonicalize(existing.url()).equals(candidateUrl);
            boolean sameTitleDay = normalizeTitle(existing.title()).equals(normalizeTitle(candidate.title()))
                    && dayKey(existing).equals(dayKey(candidate));
            if ((sameUrl || sameTitleDay)
                    && (existing.content() == null || existing.content().isBlank())) {
                kept.set(i, candidate);
                return;
            }
        }
    }

    private boolean isNearDuplicate(List<NormalizedArticle> kept, NormalizedArticle candidate,
                                    String normalizedTitle, String day) {
        Set<String> candidateTokens = tokenize(normalizedTitle);
        if (candidateTokens.isEmpty()) {
            return false;
        }
        for (NormalizedArticle existing : kept) {
            if (!dayKey(existing).equals(day)) continue;
            if (!safeEquals(existing.category(), candidate.category())) continue;
            double similarity = jaccard(candidateTokens, tokenize(normalizeTitle(existing.title())));
            if (similarity >= NEAR_DUPLICATE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokenize(String normalizedTitle) {
        Set<String> tokens = new HashSet<>();
        for (String t : normalizedTitle.split(" ")) {
            if (!t.isBlank()) tokens.add(t);
        }
        return tokens;
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private String dayKey(NormalizedArticle article) {
        if (article.publishedAt() == null) return "unknown";
        return LocalDate.ofInstant(article.publishedAt(), ZoneOffset.UTC).toString();
    }

    private boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
