package com.nia.personalization;

import com.nia.articles.ArticleRepository;
import com.nia.articles.model.Article;
import com.nia.bookmarks.BookmarkRepository;
import com.nia.config.NiaProperties;
import com.nia.history.ReadingHistoryRepository;
import com.nia.preferences.PreferencesRepository;
import com.nia.preferences.model.UserPreferences;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Transparent, rule-based feed ranking (no ML). Each candidate article is scored
 * by recency, favorite-category match, reading interest, and bookmark-source
 * affinity, using weights from configuration. Runs at request time over the most
 * recent candidate pool.
 */
@Service
public class PersonalizationService {

    private static final int CANDIDATE_POOL_SIZE = 200;
    private static final int RECENT_READS_WINDOW = 50;
    private static final double RECENCY_WINDOW_HOURS = 48.0;

    private final ArticleRepository articleRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PreferencesRepository preferencesRepository;
    private final double wRecency, wFavorite, wInterest, wBookmark;

    public PersonalizationService(ArticleRepository articleRepository,
                                  ReadingHistoryRepository readingHistoryRepository,
                                  BookmarkRepository bookmarkRepository,
                                  PreferencesRepository preferencesRepository,
                                  NiaProperties props) {
        this.articleRepository = articleRepository;
        this.readingHistoryRepository = readingHistoryRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.preferencesRepository = preferencesRepository;
        List<Double> w = props.getPersonalization().getWeights();
        this.wRecency = weightAt(w, 0, 0.5);
        this.wFavorite = weightAt(w, 1, 0.25);
        this.wInterest = weightAt(w, 2, 0.15);
        this.wBookmark = weightAt(w, 3, 0.1);
    }

    /** The user's personalized feed, highest-scoring first. */
    public List<Article> personalizedFeed(UUID userId) {
        UserPreferences prefs = preferencesRepository.findById(userId).orElse(null);
        List<String> languages = languagesOf(prefs);
        Set<String> favorites = favoritesOf(prefs);

        List<Article> pool = articleRepository.findRecentByLanguages(
                languages, PageRequest.of(0, CANDIDATE_POOL_SIZE));

        Set<String> bookmarkedSources = bookmarkedSources(userId);
        double[] interestByCategory = interestShares(userId);

        return pool.stream()
                .sorted((a, b) -> Double.compare(
                        score(b, favorites, bookmarkedSources, interestByCategory),
                        score(a, favorites, bookmarkedSources, interestByCategory)))
                .toList();
    }

    private double score(Article article, Set<String> favorites, Set<String> bookmarkedSources,
                         double[] interestByCategory) {
        double recency = recencyScore(article.getPublishedAt());
        double favorite = favorites.contains(article.getCategory()) ? 1.0 : 0.0;
        double interest = interestFor(article.getCategory(), interestByCategory);
        double bookmark = bookmarkedSources.contains(article.getSource()) ? 1.0 : 0.0;
        return wRecency * recency + wFavorite * favorite + wInterest * interest + wBookmark * bookmark;
    }

    private double recencyScore(Instant publishedAt) {
        if (publishedAt == null) return 0.0;
        double ageHours = Duration.between(publishedAt, Instant.now()).toMinutes() / 60.0;
        if (ageHours <= 0) return 1.0;
        return Math.max(0.0, 1.0 - (ageHours / RECENCY_WINDOW_HOURS));
    }

    // The interest model is a simple per-category share of the user's recent reads.
    private double[] interestShares(UUID userId) {
        List<String> recent = readingHistoryRepository.findRecentReadCategories(userId, RECENT_READS_WINDOW);
        double[] shares = new double[com.nia.news.CategoryMapper.NIA_CATEGORIES.size()];
        if (recent.isEmpty()) return shares;
        for (String category : recent) {
            int idx = com.nia.news.CategoryMapper.NIA_CATEGORIES.indexOf(category);
            if (idx >= 0) shares[idx] += 1.0;
        }
        for (int i = 0; i < shares.length; i++) {
            shares[i] = shares[i] / recent.size();
        }
        return shares;
    }

    private double interestFor(String category, double[] shares) {
        int idx = com.nia.news.CategoryMapper.NIA_CATEGORIES.indexOf(category);
        return idx >= 0 ? shares[idx] : 0.0;
    }

    private Set<String> bookmarkedSources(UUID userId) {
        List<UUID> ids = bookmarkRepository.findArticleIdsByUser(userId);
        if (ids.isEmpty()) return Set.of();
        Set<String> sources = new HashSet<>();
        for (Article a : articleRepository.findByIdIn(ids)) {
            if (a.getSource() != null) sources.add(a.getSource());
        }
        return sources;
    }

    private List<String> languagesOf(UserPreferences prefs) {
        if (prefs == null || prefs.getLanguages() == null || prefs.getLanguages().length == 0) {
            return List.of("en");
        }
        return Arrays.asList(prefs.getLanguages());
    }

    private Set<String> favoritesOf(UserPreferences prefs) {
        if (prefs == null || prefs.getFavoriteCategories() == null) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(prefs.getFavoriteCategories()));
    }

    private static double weightAt(List<Double> weights, int index, double fallback) {
        return (weights != null && weights.size() > index && weights.get(index) != null)
                ? weights.get(index) : fallback;
    }
}
