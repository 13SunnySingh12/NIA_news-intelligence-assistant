"""POST /ai/embed — generate and store embeddings for newly-ingested articles."""
from __future__ import annotations

import logging

from fastapi import APIRouter

from app.config import settings
from app.db import queries
from app.router import AIUnavailableError, embed as run_embeddings
from app.schemas import EmbedPendingRequest, EmbedPendingResponse, EmbedRequest, EmbedResponse

log = logging.getLogger("nia.embed")
router = APIRouter()

_MAX_PENDING = 500


@router.post("/embed", response_model=EmbedResponse)
def embed_articles(request: EmbedRequest) -> EmbedResponse:
    if not request.articles:
        return EmbedResponse(embedded=0)

    texts = [item.text for item in request.articles]
    try:
        vectors = run_embeddings(texts)
    except AIUnavailableError as exc:
        # Embeddings are best-effort; the caller (ingestion) must not fail because of this.
        log.warning("Embedding batch skipped: %s", exc)
        return EmbedResponse(embedded=0)

    pairs = list(zip((item.id for item in request.articles), vectors))
    updated = queries.update_embeddings(pairs)
    return EmbedResponse(embedded=updated)


@router.post("/embed/pending", response_model=EmbedPendingResponse)
def embed_pending(request: EmbedPendingRequest) -> EmbedPendingResponse:
    """Embed a bounded batch of not-yet-embedded articles (from the DB).

    Called once per ingestion cycle so embedding volume stays within the LLM
    provider's rate limit while the backlog is worked off over time.
    """
    limit = max(1, min(request.limit, _MAX_PENDING))
    articles = queries.fetch_unembedded(limit)
    if not articles:
        return EmbedPendingResponse(embedded=0, remaining=0)

    # Embed and store in small batches so a rate limit part-way through keeps the
    # work already done, instead of discarding the whole cycle's progress.
    batch_size = max(1, settings.embed_batch_size)
    embedded = 0
    for start in range(0, len(articles), batch_size):
        batch = articles[start:start + batch_size]
        texts = [f"{a['title']}\n{a.get('description') or ''}".strip() for a in batch]
        try:
            vectors = run_embeddings(texts)
        except AIUnavailableError as exc:
            # Out of quota for now; keep what succeeded and retry next cycle.
            log.warning("Pending embed stopped after %d article(s): %s", embedded, exc)
            break
        embedded += queries.update_embeddings(list(zip((str(a["id"]) for a in batch), vectors)))

    return EmbedPendingResponse(embedded=embedded, remaining=queries.count_unembedded())
