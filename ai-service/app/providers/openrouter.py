"""OpenRouter chat provider (optional fallback) — OpenAI-compatible."""
from __future__ import annotations

from app.config import settings
from app.providers.base import OpenAICompatibleChatProvider


class OpenRouterChatProvider(OpenAICompatibleChatProvider):
    def __init__(self) -> None:
        super().__init__(
            name="OPENROUTER",
            base_url=settings.openrouter_base_url,
            api_key=settings.openrouter_api_key,
            model=settings.openrouter_model,
        )
