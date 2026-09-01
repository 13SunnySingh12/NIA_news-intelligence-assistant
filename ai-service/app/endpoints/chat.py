"""POST /ai/chat — the grounded RAG news assistant."""
from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.config import settings
from app.rag import postprocess, retrieve
from app.rag.prompt import CHAT_SYSTEM_PROMPT, build_chat_messages, build_context
from app.router import AIUnavailableError, task_router
from app.schemas import ChatRequest, ChatResponse, ChatSource

log = logging.getLogger("nia.chat")
router = APIRouter()

NO_CONTEXT_ANSWER = "I don't have any relevant articles for that question yet."


@router.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    # Retrieve grounding articles (embedding failure => assistant unavailable).
    try:
        retrieved = retrieve.retrieve(request.question, settings.rag_top_k)
    except Exception as exc:
        # Any provider failure is logged server-side and returned sanitized.
        log.warning("%s failed: %s", "assistant_unavailable", type(exc).__name__)
        raise HTTPException(status_code=503, detail="assistant_unavailable")

    # Empty-retrieval guard: never call the LLM with no grounding.
    if not retrieved:
        return ChatResponse(answer=NO_CONTEXT_ANSWER, sources=[])

    context = build_context(retrieved)
    conversation = [message.model_dump() for message in (request.conversation or [])]
    messages = build_chat_messages(request.question, context, conversation)

    try:
        # Chatbot task -> Gemini (with OpenRouter fallback).
        answer = task_router.run("chat", messages, system=CHAT_SYSTEM_PROMPT)
    except Exception as exc:
        # Any provider failure is logged server-side and returned sanitized.
        log.warning("%s failed: %s", "assistant_unavailable", type(exc).__name__)
        raise HTTPException(status_code=503, detail="assistant_unavailable")

    note = postprocess.freshness_note(retrieved, settings.rag_freshness_days)
    if note:
        answer = note + answer

    sources = postprocess.map_sources(answer, retrieved)
    return ChatResponse(answer=answer, sources=[ChatSource(**source) for source in sources])
