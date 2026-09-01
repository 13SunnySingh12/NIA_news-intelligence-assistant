-- =============================================================================
-- NIA — Migration: operations (background job tracking)
-- Backend-owned, long-running operations (e.g. news refresh) record their state
-- here so the frontend can reconnect and show real status after the user
-- minimizes, switches tabs, navigates away, or returns later.
-- =============================================================================

CREATE TABLE IF NOT EXISTS operations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    type         TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    progress     INT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    current_step TEXT,
    result       TEXT,   -- small JSON payload, e.g. {"newArticles": 12}
    error        TEXT,   -- user-safe message only; never a stack trace
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS operations_user_status_idx      ON operations (user_id, status);
CREATE INDEX IF NOT EXISTS operations_user_type_status_idx ON operations (user_id, type, status);
CREATE INDEX IF NOT EXISTS operations_created_idx          ON operations (created_at DESC);

-- A user may read only their own operations. Writes happen through the backend
-- (service role, which bypasses RLS); the browser never writes directly.
ALTER TABLE operations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS operations_select ON operations;
CREATE POLICY operations_select ON operations
    FOR SELECT USING (auth.uid() = user_id);
