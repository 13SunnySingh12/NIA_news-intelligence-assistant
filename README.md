# NIA — Personalized News Intelligence Assistant

A full-stack, AI-augmented news app. NIA aggregates news from multiple free
providers, de-duplicates and categorizes it, personalizes a feed, and adds
on-demand AI summaries, semantic search, and a grounded RAG assistant that cites
real articles.

- **Frontend:** React + Vite (JavaScript/JSX), Tailwind CSS, React Router
- **Backend:** Java 21 + Spring Boot 3 (REST, JWT, scheduling, WebClient)
- **AI service:** Python 3.12 + FastAPI (embeddings, semantic search, RAG)
- **Data + Auth:** Supabase (Postgres + `pgvector`, Auth, RLS)

See [`docs/architecture.md`](docs/architecture.md) for how the pieces fit.

## Repository layout

```
NIA/
├── frontend/      React app (Vite, JS/JSX, Tailwind)
├── backend/       Spring Boot API (Maven)
├── ai-service/    FastAPI AI service (embeddings, RAG)
├── database/      schema.sql, rls.sql, migrations/
├── docker/        Dockerfiles + docker-compose for the two backends
├── docs/          architecture + provider notes
└── README.md
```

## What you need to run it fully

NIA degrades gracefully, so you can bring it up in tiers:

| You want… | You need |
|---|---|
| Sign in, preferences | A Supabase project (URL + anon key + JWT secret + DB connection) |
| Feed, categories, keyword search, bookmarks, trending | + at least one **news** key (GNews or NewsData) |
| AI summaries, semantic search, the assistant | + at least one **LLM** key (Gemini is easiest — it also does embeddings) |

Secrets stay server-side. The browser only ever gets the Supabase URL + anon key.

## Prerequisites

- Node 18+ and npm
- Java 21 + Maven
- Python 3.11+ (3.12 used here)
- A free Supabase project
- (Optional) Docker

## 1. Set up Supabase

1. Create a project at https://supabase.com.
2. In the SQL editor, run — in order — the files in `database/`:
   `schema.sql` → `rls.sql`.
   (`schema.sql` enables `pgvector` and creates the tables/indexes.)
3. **Auth → Providers:** enable **Email**, **Google**, and **GitHub**.
   (NIA intentionally offers only Google and GitHub social sign-in — no Apple.)
4. Collect these from **Project Settings**:
   - `SUPABASE_URL`, `SUPABASE_ANON_KEY` (API)
   - `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_JWT_SECRET` (API / JWT)
   - The database connection string (Database → Connection string) for
     `SPRING_DATASOURCE_*` and `DATABASE_URL`.

## 2. Configure environment

There is **one** `.env` at the repo root, shared by all services. Create it and
fill in the variables listed in
[docs/environment.md](docs/environment.md) — that table gives every variable's
purpose, whether it is required, and whether it is safe to expose.

Loading: Spring Boot reads it via a small `EnvironmentPostProcessor`, FastAPI via
`pydantic-settings`, and Vite via `envDir: '..'` — and Vite only exposes `VITE_*`
keys to the browser, so backend secrets in the same file never reach the client.
No `.env*` file is ever committed.

Get free API keys as needed — see [`docs/api-research.md`](docs/api-research.md)
and [`docs/llm-research.md`](docs/llm-research.md) for links.

## 3. Run locally

**AI service (FastAPI)** — http://localhost:8000
```bash
cd ai-service
python -m venv .venv && . .venv/Scripts/activate   # (bash: source .venv/bin/activate)
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

**Backend (Spring Boot)** — http://localhost:8080
```bash
cd backend
mvn spring-boot:run
```

**Frontend (Vite)** — http://localhost:5173
```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173, create an account, and you're in.

### Or with Docker (the two backends)

```bash
# set FASTAPI_BASE_URL=http://ai-service:8000 in .env first
docker compose -f docker/docker-compose.yml up --build
```

Both images build with the **repository root** as their context, so the single
root `.dockerignore` governs both. Building either one directly uses the same
context:

```bash
docker build -f docker/backend.Dockerfile -t nia-backend .
```

Neither image contains `.env`, source, tests, docs, or build output — the backend
image is a JRE plus the jar, the AI service is the `app/` package plus its
dependencies. Both run as an unprivileged user (`uid 10001`).

## 4. Tests

```bash
cd backend    && mvn test       # JUnit: providers, aggregator fallback, dedup, JWT,
                                #        category validation, pagination clamps, rate limiting
cd ai-service && pytest         # health, token guard, citation mapping, empty-retrieval, fallback
cd frontend   && npm test       # Vitest + RTL: protected routes, API client, signup validation
cd frontend   && npm run build  # production build
```

## 5. Scheduled jobs

Both run inside Spring Boot — no extra worker or scheduler service.

| Job | Default | Setting |
|---|---|---|
| News ingestion (all 9 categories, all providers) | every 2 hours | `NIA_INGEST_CRON` |
| Article retention (deletes stale articles) | hourly, at :05 | `NIA_RETENTION_CRON` |

Retention **deletes** articles older than `NIA_ARTICLE_RETENTION_DAYS` (default 7)
unless a user bookmarked or read them — this is what keeps the database inside a
free Supabase plan. On startup the log prints the parsed schedule and the next
run times, so the cadence can be confirmed rather than assumed:

```
NIA ingestion scheduled | cron='0 0 */2 * * *' | next runs: 02:00, 04:00, 06:00
```

Note: free hosting tiers sleep an idle service, and a sleeping service runs no
scheduled jobs. Keep the backend awake (e.g. an uptime pinger on `/api/health`)
if you want ingestion to continue while nobody is using the site.

## 6. Deploy

- **Frontend → Vercel:** import `frontend/`, set the three `VITE_*` vars.
- **Backend → Render (Web Service):** Java/Docker, set all Supabase + news + LLM
  + internal-token vars. Health check `/api/health`.
- **AI service → Render (Web Service):** Docker, set `DATABASE_URL`, LLM keys,
  and `NIA_INTERNAL_TOKEN`. Health check `/health`.
- **Supabase:** already hosts the DB, auth, and vectors.

Set `NIA_CORS_ALLOWED_ORIGINS` to your deployed frontend origin, and point
`VITE_API_BASE_URL` and `FASTAPI_BASE_URL` at the deployed backends.

## Features

Personalized feed · category browsing · keyword + semantic search · article
detail with on-demand short/detailed AI summaries · grounded RAG assistant with
cited sources · bookmarks · trending · reading-history-driven personalization ·
preferences (categories, languages, countries) · Supabase email + Google +
GitHub auth.

## Design notes

- One `NewsProvider` interface behind a priority list — add a provider without
  touching the frontend or schema.
- Rule-based, explainable personalization (no ML model).
- RAG sources come from retrieval, not from the model's output; empty retrieval
  short-circuits before calling the LLM.
- Long-running work (news refresh) is a **backend-owned operation**: `POST
  /api/news/refresh` returns an operation id immediately and runs on a small
  `@Async` executor, tracking state in the `operations` table. The frontend polls
  `GET /api/operations/{id}` only for UI status, so the work continues — and is
  recovered on return — no matter what the browser does.
- No Kafka/Redis/Kubernetes/extra vector DB — deliberately kept small enough for
  one developer to build, run, and explain.
