package com.nia.trending;

import com.nia.articles.ArticleDto;
import com.nia.articles.ArticleRepository;
import com.nia.articles.model.Article;
import com.nia.bookmarks.BookmarkRepository;
import com.nia.common.ApiException;
import com.nia.news.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Trending = most-read articles in the last 24 hours, optionally by category. */
@Service
public class TrendingService {

    private static final int DEFAULT_LIMIT = 20;

    private final ArticleRepository articleRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CategoryMapper categoryMapper;

    public TrendingService(ArticleRepository articleRepository, BookmarkRepository bookmarkRepository,
                           CategoryMapper categoryMapper) {
        this.articleRepository = articleRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<ArticleDto> getTrending(String userId, String category) {
        String normalized = null;
        if (category != null && !category.isBlank()) {
            if (!categoryMapper.isValid(category)) {
                throw ApiException.badRequest("Unknown category.");
            }
            normalized = category.toLowerCase();
        }

        List<Article> trending = articleRepository.findTrending(normalized, DEFAULT_LIMIT);
        Set<UUID> bookmarked = new HashSet<>(
                bookmarkRepository.findArticleIdsByUser(UUID.fromString(userId)));

        List<ArticleDto> result = new ArrayList<>();
        for (Article a : trending) {
            result.add(ArticleDto.of(a, false, bookmarked.contains(a.getId())));
        }
        return result;
    }
}
