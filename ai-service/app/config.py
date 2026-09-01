"""Environment-loaded settings for the AI service.

Every model id and provider base URL is configurable so that shifting free-tier
model names never require a code change (news/LLM free tiers change often).
"""
from __future__ import annotations

from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

# The single root .env (NIA/.env), shared by all services during local development.
# In production, real environment variables (Render) take precedence over any file.
_ROOT_ENV = Path(__file__).resolve().parents[2] / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=str(_ROOT_ENV), extra="ignore")

    # --- Internal service auth ---
    # Same shared secret Spring Boot sends in the X-Internal-Token header. Read from
    # NIA_INTERNAL_TOKEN so both services use one variable name.
    internal_token: str = Field(default="change-me", validation_alias="NIA_INTERNAL_TOKEN")

    # --- Database (Supabase Postgres, libpq URL) ---
    database_url: str = "postgresql://postgres:postgres@localhost:5432/postgres"

    # --- Task -> provider routing ---
    # Each AI task picks the provider best suited to it. AI_FALLBACK_PROVIDER is a
    # comma-separated chain tried in order when the primary fails, so the documented
    # chain (e.g. chat: GEMINI -> GROQ -> OPENROUTER) is configuration, not code.
    ai_chat_provider: str = "GEMINI"
    ai_summary_provider: str = "GROQ"
    ai_classification_provider: str = "GROQ"
    ai_embed_provider: str = "GEMINI"
    ai_fallback_provider: str = "OPENROUTER"

    # --- Gemini (primary chat + embeddings) ---
    gemini_api_key: str = ""
    gemini_base_url: str = "https://generativelanguage.googleapis.com/v1beta"
    gemini_chat_model: str = "gemini-3.5-flash"

    # --- Groq (OpenAI-compatible) ---
    groq_api_key: str = ""
    groq_base_url: str = "https://api.groq.com/openai/v1"
    groq_model: str = "openai/gpt-oss-20b"

    # --- OpenRouter (OpenAI-compatible, fallback provider) ---
    openrouter_api_key: str = ""
    openrouter_base_url: str = "https://openrouter.ai/api/v1"
    openrouter_model: str = "openrouter/free"

    # --- Embeddings ---
    embed_model: str = "gemini-embedding-001"
    embed_dim: int = 768
    # Texts per embedding API call. Gemini allows up to 100, but the free tier
    # rate-limits large batches (HTTP 429), so keep this modest.
    embed_batch_size: int = 20
    enable_local_embed_fallback: bool = False
    local_embed_model: str = "sentence-transformers/all-MiniLM-L6-v2"

    # --- RAG ---
    rag_top_k: int = 8
    rag_freshness_days: int = 7

    # --- HTTP timeouts (seconds) ---
    # Gemini's latency for a grounded answer swings widely (roughly 6-18s), so a
    # tight timeout produced spurious fallbacks on healthy requests. Must stay
    # comfortably below the Spring Boot internal client timeout.
    chat_timeout: float = 35.0
    embed_timeout: float = 15.0

    def provider_for(self, task: str) -> str:
        """The configured provider name for a chat-style task."""
        mapping = {
            "chat": self.ai_chat_provider,
            "summary": self.ai_summary_provider,
            "classify": self.ai_classification_provider,
        }
        return mapping.get(task, self.ai_chat_provider).strip().upper()

    def fallback_chain(self) -> list[str]:
        """Fallback provider names, in order, from the comma-separated setting."""
        return [name.strip().upper() for name in self.ai_fallback_provider.split(",") if name.strip()]


settings = Settings()
