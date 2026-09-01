"""POST /ai/summarize — on-demand short/detailed summary, cached in the DB."""
from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.db import queries
from app.rag.prompt import SUMMARY_SYSTEM_PROMPT, build_summary_user
from app.router import AIUnavailableError, task_router
from app.schemas import SummarizeRequest, SummarizeResponse

log = logging.getLogger("nia.summarize")
router = APIRouter()

_SENTENCES = {"short": 2, "detailed": 6}


@router.post("/summarize", response_model=SummarizeResponse)
def summarize(request: SummarizeRequest) -> SummarizeResponse:
    # 1) Serve from cache if we've summarized this (article, length) before.
    cached = queries.get_cached_summary(request.articleId, request.length)
    if cached:
        return SummarizeResponse(text=cached, length=request.length)

    # 2) Load the article.
    article = queries.get_article(request.articleId)
    if not article:
        raise HTTPException(status_code=404, detail="article_not_found")

    # 3) Generate.
    body = (article.get("content") or article.get("description") or "")[:6000]
    if not body.strip():
        body = article.get("title") or ""
    user_prompt = build_summary_user(
        article.get("title") or "",
        article.get("source") or "",
        body,
        _SENTENCES[request.length],
    )
    try:
        # Summarization task -> Groq (fast), with OpenRouter fallback.
        text = task_router.run("summary", [{"role": "user", "content": user_prompt}],
                               system=SUMMARY_SYSTEM_PROMPT)
    except Exception as exc:
        # Any provider failure is logged server-side and returned sanitized.
        log.warning("%s failed: %s", "summary_unavailable", type(exc).__name__)
        raise HTTPException(status_code=503, detail="summary_unavailable")

    # 4) Cache and return.
    queries.save_summary(request.articleId, request.length, text, "auto")
    return SummarizeResponse(text=text, length=request.length)
