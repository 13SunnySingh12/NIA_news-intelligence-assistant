# NIA Architecture

A short, practical map of how NIA fits together. The full product spec lives in
the root design document; this file focuses on how the running system behaves.

## Services

```
React (Vite, JS)  ──HTTPS+JWT──►  Spring Boot (Java 21)  ──REST──►  FastAPI (Python 3.12)
                                        │                                  │
                                        ▼                                  ▼
                                  Supabase Postgres  ◄────────────────  pgvector
                                  (Auth + RLS + data)                  embeddings / RAG
                                        ▲
                          News providers (GNews, NewsData,
                          Guardian, Currents, Google News RSS)
```

- **Frontend** — UI, routing, Supabase auth, and all user interaction. It talks
  only to Spring Boot. It never calls news providers, LLMs, or FastAPI directly.
- **Spring Boot** — the single API the browser sees. News aggregation +
  deduplication + categorization, scheduled ingestion, articles/bookmarks/
  history/trending/preferences, rule-based personalization, and a thin proxy to
  the AI service. Verifies the Supabase JWT on every request.
- **FastAPI** — all LLM/embedding work: embeddings, semantic search over
  pgvector, on-demand summaries, and the RAG chat endpoint. Reached only by
  Spring Boot, guarded by a shared `X-Internal-Token`.
- **Supabase** — one Postgres database (with `pgvector`), authentication, and
  row-level security. No other datastore.

## Request flows

- **Feed** — `GET /api/articles` → Spring Boot ranks the recent candidate pool
  by recency, favorite categories, reading interest, and bookmark-source
  affinity, then returns a page.
- **Summary** — `POST /api/assistant/summarize` → Spring Boot → FastAPI. Cached
  in the `summaries` table; the same `(article, length)` is never regenerated.
- **Semantic search** — `GET /api/articles/search?mode=semantic` → FastAPI
  embeds the query, runs a pgvector cosine search, returns ids; Spring Boot
  hydrates full rows. Falls back to keyword search if the AI service is down.
- **Assistant (RAG)** — `POST /api/assistant/chat` → FastAPI embeds the
  question, retrieves the top-K articles, builds a grounded prompt, calls the
  LLM, and returns the answer plus sources. Sources come from the retrieval
  step, never from URLs the model wrote.

## Ingestion

A Spring `@Scheduled` cron job (default every 10 min, `NIA_INGEST_CRON`) processes
every NIA category. For each category it queries **all** configured, non-exhausted
providers in the same cycle (GNews, NewsData, Guardian, Currents, Google News RSS),
combines the results, and deduplicates by canonical URL and a source-independent
`content_hash` (normalized title + UTC day) — so the same story from several
providers is stored once. Per-provider daily caps skip a provider near its
free-tier limit, and one provider's failure never stops the others. An
`AtomicBoolean` prevents overlapping cycles, and each cycle logs a summary
(providers, fetched, duplicatesRemoved, new, embedded, failedProviders, duration).
Embeddings are generated in a bounded batch per cycle (`NIA_EMBED_MAX_PER_CYCLE`,
default 100) so the embedding provider's rate limit is respected while the backlog
is worked off over cycles; categorization is rule-based (from the query category)
and summaries stay on-demand. No message broker or worker pool. A manual,
rate-limited `POST /api/news/refresh` runs the same pass on demand.

## Embeddings & dimensions

- Primary: Gemini `gemini-embedding-001` requested at `outputDimensionality=768`
  and L2-normalized in code (truncated Gemini vectors aren't unit-normalized).
- The `articles.embedding` column is `VECTOR(768)`; `EMBED_DIM` must match it.
- The local `sentence-transformers` fallback (384 dims) is **off by default**.
  Because one column can't mix dimensions, promoting it to primary means
  migrating the column and re-embedding every row. Keep all rows on one model.

## Security model

- The service-role key and provider secrets are backend-only; the browser sees
  only the Supabase URL + anon key.
- RLS restricts `user_preferences`, `bookmarks`, and `reading_history` to their
  owner. The backend also scopes every query by the JWT's user id, so access
  control holds even though the service role bypasses RLS.
- FastAPI is unreachable without the internal token.
- Errors are sanitized to `{ error, message }`; stack traces and provider
  details never reach the client.
