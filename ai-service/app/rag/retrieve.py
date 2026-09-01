"""Retrieval step: embed the question, then vector-search the corpus.

If the embedding provider is down we fall back to a keyword search rather than
failing the whole assistant. Either way the articles come from the database, so
answers stay grounded and sources are never invented by the model.
"""
from __future__ import annotations

import logging
from typing import Optional

from app import router
from app.db import queries

log = logging.getLogger("nia.retrieve")


def retrieve(question: str, top_k: int,
             category: Optional[str] = None,
             language: Optional[str] = None) -> list[dict]:
    """Return the top-K most relevant articles for a question (may be empty)."""
    try:
        vectors = router.embed([question])
    except router.AIUnavailableError:
        # Degrade to keyword retrieval instead of taking the assistant offline.
        log.warning("Embedding unavailable - falling back to keyword retrieval")
        return queries.keyword_search(question, top_k, category=category, language=language)

    if not vectors:
        return []
    return queries.vector_search(vectors[0], top_k, category=category, language=language)
