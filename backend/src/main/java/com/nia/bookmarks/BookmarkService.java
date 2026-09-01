package com.nia.bookmarks;

import com.nia.articles.ArticleDto;
import com.nia.articles.ArticleRepository;
import com.nia.articles.model.Article;
import com.nia.bookmarks.model.Bookmark;
import com.nia.common.ApiException;
import com.nia.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Add, remove, and list the current user's bookmarks. Idempotent by design. */
@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, ArticleRepository articleRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional
    public void add(String userId, UUID articleId) {
        UUID uid = UUID.fromString(userId);
        if (!articleRepository.existsById(articleId)) {
            throw ApiException.notFound("That article couldn't be found.");
        }
        // Idempotent: adding an existing bookmark is a no-op, preventing duplicate errors.
        if (!bookmarkRepository.existsByIdUserIdAndIdArticleId(uid, articleId)) {
            bookmarkRepository.save(new Bookmark(uid, articleId));
        }
    }

    @Transactional
    public void remove(String userId, UUID articleId) {
        bookmarkRepository.deleteByIdUserIdAndIdArticleId(UUID.fromString(userId), articleId);
    }

    public PageResponse<ArticleDto> list(String userId, int page, int size) {
        UUID uid = UUID.fromString(userId);
        Page<Bookmark> bookmarks = bookmarkRepository.findByIdUserIdOrderByCreatedAtDesc(
                uid, PageRequest.of(page, size));

        List<UUID> ids = bookmarks.getContent().stream()
                .map(b -> b.getId().getArticleId())
                .toList();

        Map<UUID, Article> byId = new LinkedHashMap<>();
        for (Article a : articleRepository.findByIdIn(ids)) {
            byId.put(a.getId(), a);
        }

        List<ArticleDto> content = new ArrayList<>();
        for (UUID id : ids) {
            Article a = byId.get(id);
            if (a != null) {
                content.add(ArticleDto.of(a, false, true));
            }
        }
        return PageResponse.from(bookmarks, content);
    }
}
