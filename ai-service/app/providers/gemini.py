"""Google Gemini chat provider (primary)."""
from __future__ import annotations

from typing import Optional

import httpx

from app.config import settings
from app.providers.base import ChatProvider


class GeminiChatProvider(ChatProvider):
    name = "GEMINI"

    def __init__(self) -> None:
        self.api_key = settings.gemini_api_key
        self.base_url = settings.gemini_base_url.rstrip("/")
        self.model = settings.gemini_chat_model

    def available(self) -> bool:
        return bool(self.api_key)

    def chat(self, messages: list[dict], system: Optional[str] = None) -> str:
        contents = []
        for message in messages:
            role = "model" if message.get("role") == "assistant" else "user"
            contents.append({"role": role, "parts": [{"text": message.get("content", "")}]})

        body: dict = {
            "contents": contents,
            "generationConfig": {"temperature": 0.2},
        }
        if system:
            body["systemInstruction"] = {"parts": [{"text": system}]}

        response = httpx.post(
            f"{self.base_url}/models/{self.model}:generateContent",
            headers={"x-goog-api-key": self.api_key, "Content-Type": "application/json"},
            json=body,
            timeout=settings.chat_timeout,
        )
        response.raise_for_status()
        data = response.json()

        candidates = data.get("candidates") or []
        if not candidates:
            raise ValueError("Gemini returned no candidates")
        parts = candidates[0].get("content", {}).get("parts") or []
        text = "".join(part.get("text", "") for part in parts).strip()
        if not text:
            raise ValueError("Gemini returned an empty response")
        return text
