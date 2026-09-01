"""POST /ai/classify — classify a news article into a NIA category.

Used for articles whose provider category is missing or unmappable; articles
fetched by category already carry a reliable label, so this stays cheap.
"""
from __future__ import annotations

import logging

from fastapi import APIRouter, HTTPException

from app.router import AIUnavailableError, task_router
from app.schemas import ClassifyRequest, ClassifyResponse

log = logging.getLogger("nia.classify")
router = APIRouter()

# The canonical NIA categories. The model must answer with exactly one of these.
NIA_CATEGORIES = [
    "technology", "business", "world", "india", "science",
    "sports", "health", "entertainment", "politics",
]

CLASSIFY_SYSTEM_PROMPT = (
    "You are a news classifier. Classify the article into exactly one category from "
    f"this list: {', '.join(NIA_CATEGORIES)}. Reply with the category word only."
)


def _match_category(raw: str) -> str:
    """Map a model reply onto a canonical NIA category, defaulting to 'world'."""
    text = (raw or "").strip().lower()
    for category in NIA_CATEGORIES:
        if category in text:
            return category
    return "world"


@router.post("/classify", response_model=ClassifyResponse)
def classify(request: ClassifyRequest) -> ClassifyResponse:
    text = f"{request.title}\n{request.description or ''}".strip()
    user_prompt = f"Classify this article:\n{text[:2000]}"
    try:
        raw = task_router.run("classify", [{"role": "user", "content": user_prompt}],
                              system=CLASSIFY_SYSTEM_PROMPT)
    except Exception as exc:
        # Any provider failure is logged server-side and returned sanitized.
        log.warning("%s failed: %s", "classification_unavailable", type(exc).__name__)
        raise HTTPException(status_code=503, detail="classification_unavailable")

    return ClassifyResponse(category=_match_category(raw))
