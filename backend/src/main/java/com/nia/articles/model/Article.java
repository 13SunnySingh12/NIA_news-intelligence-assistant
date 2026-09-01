package com.nia.articles.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping of the {@code articles} table. The {@code embedding} column is
 * intentionally not mapped here — it is owned and written by the FastAPI AI
 * service, and Spring Boot never reads or writes vectors.
 */
@Entity
@Table(name = "articles")
public class Article {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "canonical_url", nullable = false)
    private String canonicalUrl;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "author")
    private String author;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "country")
    private String country;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "content")
    private String content;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "read_count", nullable = false)
    private long readCount = 0;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Article() {
        // Required by JPA; also used by the ingestion mapper.
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public long getReadCount() { return readCount; }
    public void setReadCount(long readCount) { this.readCount = readCount; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public Instant getCreatedAt() { return createdAt; }
}
