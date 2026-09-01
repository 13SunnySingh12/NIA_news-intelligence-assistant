package com.nia.history;

import com.nia.history.model.ReadingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {

    /** Categories of the user's most recent reads, newest first — used by personalization. */
    @Query(value = """
            SELECT a.category
            FROM reading_history rh
            JOIN articles a ON a.id = rh.article_id
            WHERE rh.user_id = :userId
            ORDER BY rh.read_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findRecentReadCategories(@Param("userId") UUID userId, @Param("limit") int limit);
}
