"""Task-aware provider router.

Each chat-style task (chat, summary) is routed to the provider best suited to it,
then through the configured fallback chain (AI_FALLBACK_PROVIDER, comma-separated)
on transient failures. Embeddings stay on the configured embedding provider
(Gemini) — the vector DB column is fixed at EMBED_DIM, so we never fall back to a
different-dimension embedder.

Fallback rule: switch to the fallback provider only for transient / provider-side
failures (timeouts, rate limits, 5xx, auth/quota). A malformed request (HTTP
400/404/422) is our fault, not the provider's, so it is raised without switching.
Failures are logged with the provider name and error class only — never keys.
"""
from __future__ import annotations

import logging
from typing import Optional

import httpx

from app.config import settings
from app.embeddings import gemini_embed, local_embed
from app.providers.base import ChatProvider
from app.providers.gemini import GeminiChatProvider
from app.providers.groq import GroqChatProvider
from app.providers.openrouter import OpenRouterChatProvider

log = logging.getLogger("nia.router")


class AIUnavailableError(RuntimeError):
    """Raised when no provider could satisfy a request."""


_PROVIDER_FACTORIES = {
    "GEMINI": GeminiChatProvider,
    "GROQ": GroqChatProvider,
    "OPENROUTER": OpenRouterChatProvider,
}

# HTTP statuses that mean "our request was wrong" — do NOT switch providers.
# 404 is deliberately NOT here: from an LLM provider it means "model not found",
# which another provider can still serve.
_CLIENT_ERROR_STATUS = {400, 422}

# Some providers (Gemini among them) report a bad/expired API key or exhausted
# quota as a 400. That is a provider-credential problem, not a malformed request,
# so another provider should still be tried.
_CREDENTIAL_MARKERS = ("api key", "api_key", "unauthenticated", "permission",
                       "quota", "billing", "credential", "expired")


def is_transient(exc: Exception) -> bool:
    """True if a failure is provider-side/temporary and worth a fallback attempt."""
    if isinstance(exc, (httpx.TimeoutException, httpx.NetworkError, httpx.RemoteProtocolError)):
        return True
    if isinstance(exc, httpx.HTTPStatusError):
        status = exc.response.status_code
        if status in _CLIENT_ERROR_STATUS:
            # A 400 caused by a bad key/quota is still worth a fallback attempt.
            try:
                body = exc.response.text.lower()
            except Exception:
                body = ""
            return any(marker in body for marker in _CREDENTIAL_MARKERS)
        # 401/403/429/5xx = provider auth/quota/outage -> fall back.
        return True
    # Empty/blank provider response, or an unknown error -> allow a fallback attempt.
    return True


class TaskRouter:
    """Resolves and calls the right provider for each chat-style task."""

    def __init__(self) -> None:
        self._providers: dict[str, ChatProvider] = {
            name: factory() for name, factory in _PROVIDER_FACTORIES.items()
        }

    def _chain(self, task: str) -> list[ChatProvider]:
        """The provider order for a task: primary first, then each configured fallback."""
        chain: list[ChatProvider] = []
        for name in [settings.provider_for(task), *settings.fallback_chain()]:
            provider = self._providers.get(name)
            if provider and provider.available() and provider not in chain:
                chain.append(provider)
        return chain

    def run(self, task: str, messages: list[dict], system: Optional[str] = None) -> str:
        chain = self._chain(task)
        if not chain:
            raise AIUnavailableError(f"No provider configured for task '{task}'")
        for index, provider in enumerate(chain):
            try:
                return provider.chat(messages, system=system)
            except Exception as exc:
                if not is_transient(exc):
                    # Malformed/invalid request — switching providers won't help.
                    raise
                is_last = index == len(chain) - 1
                log.warning("Task '%s' provider %s failed (%s)%s", task, provider.name,
                            type(exc).__name__, "" if is_last else " - trying fallback")
                if is_last:
                    raise AIUnavailableError(f"All providers failed for task '{task}'") from exc
        raise AIUnavailableError(f"All providers failed for task '{task}'")


task_router = TaskRouter()


def embed(texts: list[str]) -> list[list[float]]:
    """Embed with the configured embedding provider (Gemini).

    Embeddings are NOT routed to the chat fallback: the vector DB column is fixed
    at EMBED_DIM, so switching to a different-dimension model would corrupt search.
    The only fallback is the optional local model, and only when its dimension matches.
    """
    try:
        return gemini_embed.embed_texts(texts)
    except Exception as exc:
        log.warning("Gemini embedding failed: %s", type(exc).__name__)
        if settings.enable_local_embed_fallback and settings.embed_dim == 384:
            log.info("Using local embedding fallback")
            return local_embed.embed_texts(texts)
        raise AIUnavailableError("Embedding failed and no compatible fallback is available") from exc
