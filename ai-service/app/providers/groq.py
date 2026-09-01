"""Groq chat provider (secondary) — OpenAI-compatible."""
from __future__ import annotations

from app.config import settings
from app.providers.base import OpenAICompatibleChatProvider


class GroqChatProvider(OpenAICompatibleChatProvider):
    def __init__(self) -> None:
        super().__init__(
            name="GROQ",
            base_url=settings.groq_base_url,
            api_key=settings.groq_api_key,
            model=settings.groq_model,
        )
