package com.nia.history.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One read event. Powers personalization and trending; not a first-class UI screen. */
@Entity
@Table(name = "reading_history")
public class ReadingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "read_at", insertable = false, updatable = false)
    private Instant readAt;

    protected ReadingHistory() {
    }

    public ReadingHistory(UUID userId, UUID articleId) {
        this.userId = userId;
        this.articleId = articleId;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getArticleId() { return articleId; }
    public Instant getReadAt() { return readAt; }
}
