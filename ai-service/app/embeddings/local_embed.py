"""Local sentence-transformers embedding fallback (optional, 384 dims).

Off by default. Only usable when EMBED_DIM matches the local model's dimension
(384 for all-MiniLM-L6-v2); mixing dimensions in one column is not allowed, so
switching to this as primary requires migrating articles.embedding and
re-embedding everything. See docs/architecture.md.
"""
from __future__ import annotations

from app.config import settings

_model = None


def _get_model():
    global _model
    if _model is None:
        # Imported lazily so the base image doesn't need torch unless this is enabled.
        from sentence_transformers import SentenceTransformer

        _model = SentenceTransformer(settings.local_embed_model)
    return _model


def embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        return []
    model = _get_model()
    vectors = model.encode(texts, normalize_embeddings=True)
    return [vector.tolist() for vector in vectors]
