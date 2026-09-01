-- =============================================================================
-- NIA — Database Schema (Supabase PostgreSQL + pgvector)
-- Apply in the Supabase SQL editor (or via `supabase db push` / MCP migration).
-- Order: schema.sql -> rls.sql
-- =============================================================================

-- Extensions -----------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS vector;

-- Users are managed by Supabase Auth in auth.users; we only reference them.

-- User preferences ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id             UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    favorite_categories TEXT[] NOT NULL DEFAULT '{}',
    languages           TEXT[] NOT NULL DEFAULT ARRAY['en'],
    countries           TEXT[] NOT NULL DEFAULT ARRAY['us'],
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Articles --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS articles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         TEXT NOT NULL,
    description   TEXT,
    url           TEXT NOT NULL,
    canonical_url TEXT NOT NULL,
    source        TEXT NOT NULL,
    author        TEXT,
    category      TEXT NOT NULL,
    language      TEXT NOT NULL,
    country       TEXT,
    image_url     TEXT,
    content       TEXT,
    published_at  TIMESTAMPTZ NOT NULL,
    read_count    BIGINT NOT NULL DEFAULT 0,
    provider      TEXT NOT NULL,
    content_hash  TEXT,          -- source-independent fingerprint for cross-provider dedup
    embedding     VECTOR(768),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (canonical_url)
);

CREATE INDEX IF NOT EXISTS articles_published_at_idx ON articles (published_at DESC);
CREATE INDEX IF NOT EXISTS articles_category_idx     ON articles (category);
CREATE INDEX IF NOT EXISTS articles_language_idx     ON articles (language);
CREATE INDEX IF NOT EXISTS articles_content_hash_idx ON articles (content_hash);
-- HNSW retrieves nearest neighbors reliably at any corpus size (ivfflat with a
-- fixed lists count misses neighbors on a small/sparse corpus).
CREATE INDEX IF NOT EXISTS articles_embedding_idx
    ON articles USING hnsw (embedding vector_cosine_ops);

-- On-demand AI summaries (cache) ---------------------------------------------
CREATE TABLE IF NOT EXISTS summaries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id  UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    length      TEXT NOT NULL CHECK (length IN ('short','detailed')),
    text        TEXT NOT NULL,
    model       TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (article_id, length)
);

-- Bookmarks (user-owned) ------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookmarks (
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    article_id  UUID NOT NULL REFERENCES articles(id)   ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, article_id)
);
-- Covers the FK: retention and cascade deletes look articles up by article_id.
CREATE INDEX IF NOT EXISTS bookmarks_article_id_idx ON bookmarks (article_id);

-- Reading history (user-owned) ------------------------------------------------
CREATE TABLE IF NOT EXISTS reading_history (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    article_id  UUID NOT NULL REFERENCES articles(id)   ON DELETE CASCADE,
    read_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS reading_history_user_time_idx ON reading_history (user_id, read_at DESC);
-- Covers the FK: retention and cascade deletes look articles up by article_id.
CREATE INDEX IF NOT EXISTS reading_history_article_id_idx ON reading_history (article_id);

-- Background operations (job tracking) -----------------------------------------
-- Long-running, backend-owned work (e.g. news refresh) records its state here so
-- the frontend can reconnect and show real status after the user leaves/returns.
CREATE TABLE IF NOT EXISTS operations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    type         TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    progress     INT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    current_step TEXT,
    result       TEXT,
    error        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS operations_user_status_idx      ON operations (user_id, status);
CREATE INDEX IF NOT EXISTS operations_user_type_status_idx ON operations (user_id, type, status);
CREATE INDEX IF NOT EXISTS operations_created_idx          ON operations (created_at DESC);
