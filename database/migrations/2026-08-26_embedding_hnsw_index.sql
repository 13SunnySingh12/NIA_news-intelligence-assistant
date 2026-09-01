-- =============================================================================
-- NIA — Migration: switch the embedding index from ivfflat to HNSW
-- ivfflat with a fixed `lists` count misses nearest neighbors on a small/sparse
-- corpus (default probes=1 checks only one list), which made semantic search and
-- RAG return no results. HNSW retrieves reliably at any corpus size, with no
-- probes/lists tuning. Same pgvector extension — no new infrastructure.
-- =============================================================================

DROP INDEX IF EXISTS articles_embedding_idx;

CREATE INDEX IF NOT EXISTS articles_embedding_idx
    ON articles USING hnsw (embedding vector_cosine_ops);
