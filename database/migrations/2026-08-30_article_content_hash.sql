-- =============================================================================
-- NIA — Migration: article content fingerprint (cross-provider dedup)
-- A source-independent hash (normalized title + UTC day) so the same story from
-- different providers or a later cycle is not stored multiple times.
-- =============================================================================

ALTER TABLE articles ADD COLUMN IF NOT EXISTS content_hash TEXT;

CREATE INDEX IF NOT EXISTS articles_content_hash_idx ON articles (content_hash);
