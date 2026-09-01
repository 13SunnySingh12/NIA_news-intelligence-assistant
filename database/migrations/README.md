# Migrations

The initial schema lives in `../schema.sql` and `../rls.sql` — run those once on
a fresh Supabase project.

For any later schema change, add a dated, forward-only SQL file here
(e.g. `2026-09-01_add_source_index.sql`) and apply them in filename order. Keep
`../schema.sql` as the current full picture of the database.
