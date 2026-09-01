# Environment Configuration Audit

One file — the root `.env` — holds all configuration. Loading:

- **Spring Boot** — `DotenvEnvironmentPostProcessor` loads the root `.env` in local
  dev (lowest precedence, so real env vars on Render always win).
- **FastAPI** — `pydantic-settings` reads the same root `.env` (`env_file`).
- **Vite/React** — `envDir: '..'` loads the root `.env`, but **only `VITE_*` keys
  reach the browser bundle**; every other value stays server-side.

No `.env*` file is committed. This table is the authoritative list of variables —
create a `.env` at the repo root containing the ones marked **Required**.

## Audit

| Variable | Service(s) | Purpose | Required | Exposure |
|---|---|---|---|---|
| `NIA_INGEST_ENABLED` | Spring Boot | Toggle scheduled ingestion | Optional (default true) | Private |
| `NIA_INGEST_CRON` | Spring Boot | Ingestion schedule | Optional | Private |
| `NIA_INGEST_CATEGORIES` | Spring Boot | Categories to ingest | Optional | Private |
| `NIA_INGEST_COUNTRIES` | Spring Boot | Countries to ingest | Optional | Private |
| `NIA_DEFAULT_LANGUAGE` | Spring Boot | Default language | Optional | Private |
| `NIA_RAG_TOP_K` | Spring Boot | RAG retrieval size (proxied to FastAPI) | Optional | Private |
| `NIA_RETENTION_ENABLED` | Spring Boot | Toggle the article-retention sweep | Optional (default true) | Private |
| `NIA_ARTICLE_RETENTION_DAYS` | Spring Boot | Delete articles older than this, unless bookmarked/read. This is what keeps storage inside a free Supabase plan | Optional (default 7) | Private |
| `NIA_RETENTION_CRON` | Spring Boot | Retention schedule (offset from ingestion) | Optional | Private |
| `NIA_EMBED_MAX_PER_CYCLE` | Spring Boot | Cap on embeddings requested per ingestion cycle, to stay inside the embedding provider's rate limit | Optional (default 100) | Private |
| `NIA_PERSONALIZATION_WEIGHTS` | Spring Boot | Feed scoring weights | Optional | Private |
| `SUPABASE_URL` | Supabase config | Project URL (mirrors `VITE_SUPABASE_URL`) | Optional server-side | Private |
| `SUPABASE_ANON_KEY` | Supabase config | Public anon key (mirrors `VITE_*`) | Optional server-side | Public value |
| `SUPABASE_SERVICE_ROLE_KEY` | Backend (reserved) | Privileged Supabase ops; current code uses the direct DB connection instead | Optional | **Secret** |
| `SUPABASE_JWT_SECRET` | Spring Boot | **Verify Supabase JWTs** (actively used) | **Required** | **Secret** |
| `SPRING_DATASOURCE_URL` | Spring Boot | JDBC Postgres URL | **Required** | Private |
| `SPRING_DATASOURCE_USERNAME` | Spring Boot | DB user | **Required** | Private |
| `SPRING_DATASOURCE_PASSWORD` | Spring Boot | DB password | **Required** | **Secret** |
| `DATABASE_URL` | FastAPI | libpq Postgres URL (pgvector) | **Required** | **Secret** |
| `NEWS_PROVIDER_PRIMARY/SECONDARY/FALLBACK` | Spring Boot | Provider priority | Optional | Private |
| `GNEWS_API_KEY` … `NEWSAPI_API_KEY` | Spring Boot | News provider keys | Optional (≥1 for a live feed) | **Secret** |
| `AI_CHAT_PROVIDER` / `AI_SUMMARY_PROVIDER` / `AI_CLASSIFICATION_PROVIDER` / `AI_EMBED_PROVIDER` | FastAPI | Provider per AI task | Optional | Private |
| `AI_FALLBACK_PROVIDER` | FastAPI | Comma-separated fallback chain (e.g. `GROQ,OPENROUTER`) | Optional | Private |
| `GEMINI_API_KEY` | FastAPI | Chat + embeddings | Optional (recommended) | **Secret** |
| `GROQ_API_KEY` / `OPENROUTER_API_KEY` | FastAPI | Fallback chat | Optional | **Secret** |
| `GEMINI_CHAT_MODEL` / `GROQ_MODEL` / `OPENROUTER_MODEL` | FastAPI | Model ids (overridable) | Optional | Private |
| `EMBED_MODEL` / `EMBED_DIM` | FastAPI | Embedding model + vector size (`EMBED_DIM` must match the `VECTOR(n)` column) | Optional | Private |
| `EMBED_BATCH_SIZE` | FastAPI | Texts per embedding call; kept modest because large batches trip the free-tier rate limit | Optional (default 20) | Private |
| `CHAT_TIMEOUT` / `EMBED_TIMEOUT` | FastAPI | HTTP timeouts in seconds; `CHAT_TIMEOUT` must stay below the Spring Boot client timeout | Optional | Private |
| `ENABLE_LOCAL_EMBED_FALLBACK` | FastAPI | Local embedding fallback toggle | Optional | Private |
| `FASTAPI_BASE_URL` | Spring Boot | Where to reach FastAPI | **Required** | Private |
| `NIA_INTERNAL_TOKEN` | Spring Boot + FastAPI | Shared internal auth (`X-Internal-Token`) | **Required** | **Secret** |
| `NIA_CORS_ALLOWED_ORIGINS` | Spring Boot | Allowed frontend origins | **Required** | Private |
| `VITE_SUPABASE_URL` | Frontend | Supabase URL | **Required** | **Public** |
| `VITE_SUPABASE_ANON_KEY` | Frontend | Supabase anon key | **Required** | **Public** |
| `VITE_API_BASE_URL` | Frontend | Backend base URL | Optional (default localhost) | **Public** |

## Startup validation

- **Spring Boot** fails fast (naming the variable, never the value) if
  `SUPABASE_JWT_SECRET` or `NIA_INTERNAL_TOKEN` is missing, and warns if no news
  provider key is set.
- **FastAPI** warns clearly if the internal token is the default, if
  `DATABASE_URL` is unconfigured, or if no LLM key is present.
- **Frontend** warns in the dev console (and disables auth gracefully) if
  `VITE_SUPABASE_URL` / `VITE_SUPABASE_ANON_KEY` are missing.

## Security boundary (verified)

Only `VITE_*` keys are compiled into the browser bundle. A build-time check
confirmed the internal token, service-role key, and DB password do **not** appear
in `frontend/dist`, while the public anon key does.
