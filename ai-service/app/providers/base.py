"""Chat provider abstraction and a shared OpenAI-compatible implementation.

Groq and OpenRouter both expose the OpenAI chat-completions surface,
so they share one implementation that differs only by base URL, key, and model.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Optional

import httpx

from app.config import settings


class ChatProvider(ABC):
    """A text-chat backend. `messages` are {role, content} dicts (user/assistant)."""

    name: str

    @abstractmethod
    def available(self) -> bool:
        """True if this provider is configured (has an API key)."""

    @abstractmethod
    def chat(self, messages: list[dict], system: Optional[str] = None) -> str:
        """Return the assistant's reply text, or raise on failure."""


class OpenAICompatibleChatProvider(ChatProvider):
    def __init__(self, name: str, base_url: str, api_key: str, model: str):
        self.name = name
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.model = model

    def available(self) -> bool:
        return bool(self.api_key)

    def chat(self, messages: list[dict], system: Optional[str] = None) -> str:
        payload_messages = []
        if system:
            payload_messages.append({"role": "system", "content": system})
        payload_messages.extend(messages)

        response = httpx.post(
            f"{self.base_url}/chat/completions",
            headers={"Authorization": f"Bearer {self.api_key}"},
            json={
                "model": self.model,
                "messages": payload_messages,
                "temperature": 0.2,
            },
            timeout=settings.chat_timeout,
        )
        response.raise_for_status()
        data = response.json()
        content = data["choices"][0]["message"]["content"]
        if not content or not content.strip():
            raise ValueError(f"{self.name} returned an empty response")
        return content.strip()
