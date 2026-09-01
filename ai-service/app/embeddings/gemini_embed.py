"""Gemini embeddings (primary). Uses gemini-embedding-001 truncated to EMBED_DIM.

Truncated Gemini embeddings (dim != 3072) are not unit-normalized by the API, so
we L2-normalize here to keep cosine distance meaningful.
"""
from __future__ import annotations

import math
import time

import httpx

from app.config import settings

_TRANSIENT_STATUS = {429, 500, 502, 503, 504}


# Gemini's batchEmbedContents accepts at most 100 requests per call. The
# configured batch size is clamped to this hard API limit.
_MAX_BATCH = 100


def _l2_normalize(vector: list[float]) -> list[float]:
    norm = math.sqrt(sum(v * v for v in vector))
    if norm == 0:
        return vector
    return [v / norm for v in vector]


def embed_texts(texts: list[str]) -> list[list[float]]:
    """Return one EMBED_DIM-length vector per input text (chunked to Gemini's batch limit)."""
    if not texts:
        return []
    if not settings.gemini_api_key:
        raise RuntimeError("Gemini API key is not configured")

    batch_size = max(1, min(settings.embed_batch_size, _MAX_BATCH))
    vectors: list[list[float]] = []
    for start in range(0, len(texts), batch_size):
        chunk = texts[start:start + batch_size]
        try:
            vectors.extend(_embed_chunk(chunk))
        except httpx.HTTPStatusError as exc:
            # Retry once on a rate limit / transient outage; a 400 is not retried.
            if exc.response.status_code not in _TRANSIENT_STATUS:
                raise
            time.sleep(2)
            vectors.extend(_embed_chunk(chunk))
        except (httpx.TimeoutException, httpx.NetworkError):
            time.sleep(2)
            vectors.extend(_embed_chunk(chunk))
    return vectors


def _embed_chunk(texts: list[str]) -> list[list[float]]:
    requests = [
        {
            "model": f"models/{settings.embed_model}",
            "content": {"parts": [{"text": text}]},
            "outputDimensionality": settings.embed_dim,
        }
        for text in texts
    ]

    response = httpx.post(
        f"{settings.gemini_base_url.rstrip('/')}/models/{settings.embed_model}:batchEmbedContents",
        headers={"x-goog-api-key": settings.gemini_api_key, "Content-Type": "application/json"},
        json={"requests": requests},
        timeout=settings.embed_timeout,
    )
    response.raise_for_status()
    data = response.json()

    embeddings = data.get("embeddings") or []
    if len(embeddings) != len(texts):
        raise ValueError("Gemini returned an unexpected number of embeddings")
    return [_l2_normalize(item.get("values", [])) for item in embeddings]
