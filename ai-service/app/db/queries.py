"""SQL for embeddings, semantic search, and summary caching."""
from __future__ import annotations

import re

from typing import Optional

from psycopg.rows import dict_row

from app.db.supabase import get_pool, vector_literal


def update_embeddings(pairs: list[tuple[str, list[float]]]) -> int:
    """Write embeddings for the given (article_id, vector) pairs. Returns rows updated."""
    if not pairs:
        return 0
    params = [(vector_literal(vector), article_id) for article_id, vector in pairs]
    with get_pool().connection() as conn:
        with conn.cursor() as cur:
            cur.executemany(
                "UPDATE articles SET embedding = %s::vector WHERE id = %s::uuid",
                params,
            )
    return len(params)


def vector_search(
    query_vector: list[float],
    top_k: int,
    category: Optional[str] = None,
    language: Optional[str] = None,
    published_after: Optional[str] = None,
) -> list[dict]:
    """Nearest articles by cosine distance, with an internal 0..1 score."""
    literal = vector_literal(query_vector)
    clauses = ["embedding IS NOT NULL"]
    params: list = [literal]  # first %s is the score expression

    if category:
        clauses.append("category = %s")
        params.append(category)
    if language:
        clauses.append("language = %s")
        params.append(language)
    if published_after:
        clauses.append("published_at >= %s::timestamptz")
        params.append(published_after)

    sql = f"""
        SELECT id, title, description, url, source, category, image_url,
               published_at, content,
               1 - (embedding <=> %s::vector) AS score
        FROM articles
        WHERE {" AND ".join(clauses)}
        ORDER BY embedding <=> %s::vector
        LIMIT %s
    """
    params.append(literal)  # ORDER BY vector
    params.append(top_k)

    with get_pool().connection() as conn:
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(sql, params)
            return cur.fetchall()


def keyword_search(
    question: str,
    top_k: int,
    category: Optional[str] = None,
    language: Optional[str] = None,
) -> list[dict]:
    """Recency-ranked keyword match, used when embeddings are unavailable.

    Returns the same row shape as vector_search so the RAG pipeline can keep
    working (grounded in real DB rows) during an embedding-provider outage.
    """
    words = [w for w in re.findall(r"[A-Za-z0-9]{3,}", question.lower())][:5]
    clauses: list[str] = []
    params: list = []
    if words:
        clauses.append("(" + " OR ".join(["title ILIKE %s OR description ILIKE %s"] * len(words)) + ")")
        for word in words:
            params.extend([f"%{word}%", f"%{word}%"])
    if category:
        clauses.append("category = %s")
        params.append(category)
    if language:
        clauses.append("language = %s")
        params.append(language)

    where = (" WHERE " + " AND ".join(clauses)) if clauses else ""
    sql = f"""
        SELECT id, title, description, url, source, category, image_url,
               published_at, content, 0.0 AS score
        FROM articles
        {where}
        ORDER BY published_at DESC
        LIMIT %s
    """
    params.append(top_k)

    with get_pool().connection() as conn:
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(sql, params)
            return cur.fetchall()


def fetch_unembedded(limit: int) -> list[dict]:
    """The most recent articles that still need an embedding."""
    with get_pool().connection() as conn:
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(
                "SELECT id, title, description FROM articles "
                "WHERE embedding IS NULL ORDER BY published_at DESC LIMIT %s",
                (limit,),
            )
            return cur.fetchall()


def count_unembedded() -> int:
    with get_pool().connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT count(*) FROM articles WHERE embedding IS NULL")
            return cur.fetchone()[0]


def get_article(article_id: str) -> Optional[dict]:
    with get_pool().connection() as conn:
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(
                "SELECT id, title, source, description, content, url, published_at "
                "FROM articles WHERE id = %s::uuid",
                (article_id,),
            )
            return cur.fetchone()


def get_cached_summary(article_id: str, length: str) -> Optional[str]:
    with get_pool().connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT text FROM summaries WHERE article_id = %s::uuid AND length = %s",
                (article_id, length),
            )
            row = cur.fetchone()
            return row[0] if row else None


def save_summary(article_id: str, length: str, text: str, model: str) -> None:
    """Cache a summary. The (article_id, length) pair is never regenerated once stored."""
    with get_pool().connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO summaries (article_id, length, text, model)
                VALUES (%s::uuid, %s, %s, %s)
                ON CONFLICT (article_id, length) DO NOTHING
                """,
                (article_id, length, text, model),
            )
