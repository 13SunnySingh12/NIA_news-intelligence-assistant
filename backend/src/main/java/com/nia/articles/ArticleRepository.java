package com.nia.articles;

import com.nia.articles.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Page<Article> findByCategoryOrderByPublishedAtDesc(String category, Pageable pageable);

    /** Batched dedup: which of these canonical URLs already exist (one query, not N). */
    @Query("SELECT a.canonicalUrl FROM Article a WHERE a.canonicalUrl IN :urls")
    List<String> findExistingCanonicalUrls(@Param("urls") Collection<String> urls);

    /** Batched dedup: which of these content hashes already exist (one query, not N). */
    @Query("SELECT a.contentHash FROM Article a WHERE a.contentHash IN :hashes")
    List<String> findExistingContentHashes(@Param("hashes") Collection<String> hashes);

    List<Article> findByIdIn(List<UUID> ids);

    @Query("SELECT a FROM Article a WHERE a.language IN :languages ORDER BY a.publishedAt DESC")
    List<Article> findRecentByLanguages(@Param("languages") List<String> languages, Pageable pageable);

    @Query("""
            SELECT a FROM Article a
            WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY a.publishedAt DESC
            """)
    Page<Article> searchByKeyword(@Param("q") String q, Pageable pageable);

    @Query(value = """
            SELECT * FROM articles
            WHERE published_at > NOW() - INTERVAL '24 hours'
              AND (CAST(:category AS text) IS NULL OR category = CAST(:category AS text))
            ORDER BY read_count DESC, published_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Article> findTrending(@Param("category") String category, @Param("limit") int limit);

    @Modifying
    @Transactional
    @Query("UPDATE Article a SET a.readCount = a.readCount + 1 WHERE a.id = :id")
    int incrementReadCount(@Param("id") UUID id);

    /**
     * Delete articles published before {@code cutoff}, except any a user still
     * refers to (bookmarked or read). Keeps the database bounded on a free tier —
     * without this the table grows forever. Returns how many rows were removed.
     */
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM articles a
            WHERE a.published_at < :cutoff
              AND NOT EXISTS (SELECT 1 FROM bookmarks b WHERE b.article_id = a.id)
              AND NOT EXISTS (SELECT 1 FROM reading_history r WHERE r.article_id = a.id)
            """, nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") java.time.Instant cutoff);
}
