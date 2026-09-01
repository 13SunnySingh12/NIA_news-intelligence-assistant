"""Post-processing: map the model's [n] citations back to retrieved articles.

The sources returned to the user come from the retrieval set, never from URLs the
model wrote. Unmapped citations are dropped; if none map, all retrieved articles
are returned (best-effort grounding).
"""
from __future__ import annotations

import re
from datetime import datetime, timedelta, timezone

_CITATION = re.compile(r"\[(\d+)\]")


def map_sources(answer: str, retrieved: list[dict]) -> list[dict]:
    """Return the retrieved articles the answer actually cites, in first-appearance order."""
    if not retrieved:
        return []

    ordered_indexes: list[int] = []
    for match in _CITATION.finditer(answer or ""):
        idx = int(match.group(1)) - 1
        if 0 <= idx < len(retrieved) and idx not in ordered_indexes:
            ordered_indexes.append(idx)

    chosen = ordered_indexes if ordered_indexes else list(range(len(retrieved)))
    sources = []
    for idx in chosen:
        article = retrieved[idx]
        sources.append({
            "id": str(article.get("id")),
            "title": article.get("title") or "",
            "source": article.get("source") or "",
            "url": article.get("url") or "",
        })
    return sources


def freshness_note(retrieved: list[dict], threshold_days: int) -> str | None:
    """A short prefix when the newest retrieved article is older than the threshold."""
    newest = None
    for article in retrieved:
        published = article.get("published_at")
        if isinstance(published, datetime):
            if newest is None or published > newest:
                newest = published
    if newest is None:
        return None
    cutoff = datetime.now(timezone.utc) - timedelta(days=threshold_days)
    if newest.tzinfo is None:
        newest = newest.replace(tzinfo=timezone.utc)
    if newest < cutoff:
        return "I couldn't find recent coverage on this, so this is based on older articles.\n\n"
    return None
