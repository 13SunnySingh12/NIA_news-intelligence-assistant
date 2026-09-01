package com.nia.bookmarks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link Bookmark}: (user_id, article_id). */
@Embeddable
public class BookmarkId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "article_id")
    private UUID articleId;

    protected BookmarkId() {
    }

    public BookmarkId(UUID userId, UUID articleId) {
        this.userId = userId;
        this.articleId = articleId;
    }

    public UUID getUserId() { return userId; }
    public UUID getArticleId() { return articleId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookmarkId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(articleId, that.articleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, articleId);
    }
}
