# LLM Provider Notes

> Free-tier model names change often. Every model id and base URL is
> configurable via environment variables so drift never requires a code change.
> Verified for this build in August 2026.

## Task -> provider routing

Each AI task uses the provider best suited to it, then walks the fallback chain
(`AI_FALLBACK_PROVIDER`, comma-separated) if that provider fails. Provider order
is configuration, not code — e.g. `AI_FALLBACK_PROVIDER=GROQ,OPENROUTER` makes
chat run Gemini -> Groq -> OpenRouter.

| Task | Provider | Base URL | Default model (override with env) |
|---|---|---|---|
| Chatbot (RAG) | Gemini | `https://generativelanguage.googleapis.com/v1beta` | `gemini-3.5-flash` (`GEMINI_CHAT_MODEL`) |
| Summarization | Groq | `https://api.groq.com/openai/v1` | `openai/gpt-oss-20b` (`GROQ_MODEL`) |
| Embeddings | Gemini | `.../v1beta` | `gemini-embedding-001` @ 768 dims (`EMBED_MODEL` / `EMBED_DIM`) |
| Fallback chain | Groq -> OpenRouter | see above | `AI_FALLBACK_PROVIDER=GROQ,OPENROUTER` |

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


