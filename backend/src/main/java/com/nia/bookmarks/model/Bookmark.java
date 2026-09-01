package com.nia.bookmarks.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A user's saved article. Access is restricted per-user by Supabase RLS. */
@Entity
@Table(name = "bookmarks")
public class Bookmark {

    @EmbeddedId
    private BookmarkId id;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Bookmark() {
    }

    public Bookmark(UUID userId, UUID articleId) {
        this.id = new BookmarkId(userId, articleId);
    }

    public BookmarkId getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}
