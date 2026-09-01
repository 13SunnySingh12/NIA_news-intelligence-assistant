-- =============================================================================
-- NIA — Row Level Security
-- User-owned tables: a user can only touch their own rows.
-- Shared read tables (articles, summaries): any authenticated user may read;
-- only the service role (backend) may write.
-- Apply AFTER schema.sql.
--
-- Note: auth.uid() is wrapped in (select ...) so Postgres evaluates it once per
-- statement instead of once per row. Semantics are identical; it avoids a large
-- per-row cost as the tables grow.
-- =============================================================================

-- ---- user_preferences -------------------------------------------------------
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_preferences_select ON user_preferences;
CREATE POLICY user_preferences_select ON user_preferences
    FOR SELECT USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS user_preferences_insert ON user_preferences;
CREATE POLICY user_preferences_insert ON user_preferences
    FOR INSERT WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS user_preferences_update ON user_preferences;
CREATE POLICY user_preferences_update ON user_preferences
    FOR UPDATE USING ((select auth.uid()) = user_id) WITH CHECK ((select auth.uid()) = user_id);

-- ---- bookmarks --------------------------------------------------------------
ALTER TABLE bookmarks ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS bookmarks_select ON bookmarks;
CREATE POLICY bookmarks_select ON bookmarks
    FOR SELECT USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS bookmarks_insert ON bookmarks;
CREATE POLICY bookmarks_insert ON bookmarks
    FOR INSERT WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS bookmarks_delete ON bookmarks;
CREATE POLICY bookmarks_delete ON bookmarks
    FOR DELETE USING ((select auth.uid()) = user_id);

-- ---- reading_history --------------------------------------------------------
ALTER TABLE reading_history ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS reading_history_select ON reading_history;
CREATE POLICY reading_history_select ON reading_history
    FOR SELECT USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS reading_history_insert ON reading_history;
CREATE POLICY reading_history_insert ON reading_history
    FOR INSERT WITH CHECK ((select auth.uid()) = user_id);

-- ---- articles ---------------------------------------------------------------
-- Readable by any authenticated user. Writes only via the service role, which
-- bypasses RLS, so no write policy is defined (default deny for anon/auth).
ALTER TABLE articles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS articles_select ON articles;
CREATE POLICY articles_select ON articles
    FOR SELECT TO authenticated USING (true);

-- ---- summaries --------------------------------------------------------------
ALTER TABLE summaries ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS summaries_select ON summaries;
CREATE POLICY summaries_select ON summaries
    FOR SELECT TO authenticated USING (true);

-- ---- operations -------------------------------------------------------------
-- A user may read only their own operations. The backend (service role) writes.
ALTER TABLE operations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS operations_select ON operations;
CREATE POLICY operations_select ON operations
    FOR SELECT USING ((select auth.uid()) = user_id);

-- Note: the service role key (used only by Spring Boot and FastAPI) bypasses
-- RLS entirely, which is how ingestion, embeddings, and summary writes happen.
