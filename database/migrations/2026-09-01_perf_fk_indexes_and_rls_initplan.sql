-- Performance: cover the foreign keys scanned by article retention and cascade
-- deletes, and evaluate auth.uid() once per statement instead of once per row.
-- Applied to the live project on 2026-09-01. Safe to re-run.

CREATE INDEX IF NOT EXISTS bookmarks_article_id_idx       ON public.bookmarks (article_id);
CREATE INDEX IF NOT EXISTS reading_history_article_id_idx ON public.reading_history (article_id);

-- RLS policies re-created with (select auth.uid()) — identical semantics.
DROP POLICY IF EXISTS user_preferences_select ON public.user_preferences;
CREATE POLICY user_preferences_select ON public.user_preferences
    FOR SELECT USING ((select auth.uid()) = user_id);
DROP POLICY IF EXISTS user_preferences_insert ON public.user_preferences;
CREATE POLICY user_preferences_insert ON public.user_preferences
    FOR INSERT WITH CHECK ((select auth.uid()) = user_id);
DROP POLICY IF EXISTS user_preferences_update ON public.user_preferences;
CREATE POLICY user_preferences_update ON public.user_preferences
    FOR UPDATE USING ((select auth.uid()) = user_id) WITH CHECK ((select auth.uid()) = user_id);
DROP POLICY IF EXISTS bookmarks_select ON public.bookmarks;
CREATE POLICY bookmarks_select ON public.bookmarks
    FOR SELECT USING ((select auth.uid()) = user_id);
DROP POLICY IF EXISTS bookmarks_insert ON public.bookmarks;
CREATE POLICY bookmarks_insert ON public.bookmarks
    FOR INSERT WITH CHECK ((select auth.uid()) = user_id);
DROP POLICY IF EXISTS bookmarks_delete ON public.bookmarks;
CREATE POLICY bookmarks_delete ON public.bookmarks
    FOR DELETE USING ((select auth.uid()) = user_id);
DROP POLICY IF EXISTS reading_history_select ON public.reading_history;
CREATE POLICY reading_history_select ON public.reading_history
    FOR SELECT USING ((select auth.uid()) = user_id);
DROP POLICY IF EXISTS reading_history_insert ON public.reading_history;
CREATE POLICY reading_history_insert ON public.reading_history
    FOR INSERT WITH CHECK ((select auth.uid()) = user_id);
DROP POLICY IF EXISTS operations_select ON public.operations;
CREATE POLICY operations_select ON public.operations
    FOR SELECT USING ((select auth.uid()) = user_id);
