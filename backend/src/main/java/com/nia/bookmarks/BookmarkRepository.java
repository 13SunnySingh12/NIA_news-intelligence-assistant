package com.nia.bookmarks;

import com.nia.bookmarks.model.Bookmark;
import com.nia.bookmarks.model.BookmarkId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

    boolean existsByIdUserIdAndIdArticleId(UUID userId, UUID articleId);

    void deleteByIdUserIdAndIdArticleId(UUID userId, UUID articleId);

    Page<Bookmark> findByIdUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT b.id.articleId FROM Bookmark b WHERE b.id.userId = :userId")
    List<UUID> findArticleIdsByUser(@Param("userId") UUID userId);
}
