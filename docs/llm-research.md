# LLM Provider Notes

> Free-tier model names change often. Every model id and base URL is
> configurable via environment variables so drift never requires a code change.
> Verified for this build in August 2026.

## Task -> provider routing

Measured on the free tiers (5 identical one-sentence summarisation calls each):

| Provider / model | Success | Avg latency |
|---|---|---|
| Groq `openai/gpt-oss-20b` | 5/5 | 525 ms |
| OpenRouter `openrouter/free` | 4/5 | 3,267 ms |
| Gemini `gemini-flash-lite-latest` | 3/3 | 818 ms |
| Gemini `gemini-flash-latest` | 3/5 | 27,386 ms |
| Gemini `gemini-3.5-flash` | 0/5 | quota exhausted (20 requests/day) |

Gemini's free-tier quota is **per model**, so the model id matters as much as the
provider. `gemini-3.5-flash` allows 20 generate-content requests per day; the
`-lite` variants have their own, far more usable allowance. `GEMINI_CHAT_MODEL`
therefore defaults to the alias `gemini-flash-lite-latest` rather than a pinned
id — three pinned ids used by earlier builds (`gemini-2.5-flash`,
`gemini-2.0-flash`, `gemini-2.5-flash-lite`) now return 404, and an alias keeps
working when Google retires a version.

Groq is therefore the primary for every chat-style task. Gemini's free tier caps
`gemini-3.5-flash` at 20 generate-content requests per day, which a news
assistant exhausts almost immediately, so it sits last in the fallback chain.

Embeddings stay on Gemini regardless: `articles.embedding` is `VECTOR(768)` and a
different provider would emit a different vector size, invalidating every stored
vector. The router deliberately refuses to fall back for embeddings.

Each AI task uses the provider best suited to it, then walks the fallback chain
(`AI_FALLBACK_PROVIDER`, comma-separated) if that provider fails. Provider order
is configuration, not code — with `AI_CHAT_PROVIDER=GROQ` and
`AI_FALLBACK_PROVIDER=OPENROUTER,GEMINI`, chat runs Groq -> OpenRouter -> Gemini.

| Task | Provider | Base URL | Default model (override with env) |
|---|---|---|---|
| Chatbot (RAG) | Groq | `https://api.groq.com/openai/v1` | `openai/gpt-oss-20b` (`GROQ_MODEL`) |
| Summarization | Groq | `https://api.groq.com/openai/v1` | `openai/gpt-oss-20b` (`GROQ_MODEL`) |
| Embeddings | Gemini | `.../v1beta` | `gemini-embedding-001` @ 768 dims (`EMBED_MODEL` / `EMBED_DIM`) |
| Fallback chain | OpenRouter -> Gemini | see above | `AI_FALLBACK_PROVIDER=OPENROUTER,GEMINI` |

Groq and OpenRouter speak the OpenAI
chat-completions format (one shared client); Gemini uses its own
`generateContent` REST shape.

## Embeddings

- Model: `gemini-embedding-001`, requested at `outputDimensionality=768` to match
  the `VECTOR(768)` column. Truncated Gemini embeddings are not unit-normalized,
  so the service L2-normalizes them before storage.
- Fallback: local `sentence-transformers/all-MiniLM-L6-v2` (384 dims), off by
  default. Enabling it as primary requires migrating the column + re-embedding.

## Routing & resilience

- Each chat-style task calls its configured provider, then each provider in the
  fallback chain, on a **transient** failure: rate limit (429), outage (5xx),
  model not found (404), timeout, network error, or auth/quota (401/403).
- A malformed request (HTTP 400/422) is **not** retried on another provider —
  switching wouldn't help, and it surfaces our own request bugs instead of
  silently masking them.
- Embeddings are never routed to a different-dimension provider (the vector DB is
  fixed at `EMBED_DIM`); the only embedding fallback is the optional local model
  when its dimension matches.
- Failures are logged with the provider name + error class only — never keys.

## Getting keys

- Gemini — https://aistudio.google.com/app/apikey (one key covers chat + embeddings)
- Groq — https://console.groq.com/keys
- OpenRouter — https://openrouter.ai/keys


