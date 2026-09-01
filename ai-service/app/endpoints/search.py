"""POST /ai/search — semantic search over the article corpus."""
from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.db import queries
from app.router import AIUnavailableError, embed as run_embeddings
from app.schemas import SearchHit, SearchRequest, SearchResponse

router = APIRouter()


@router.post("/search", response_model=SearchResponse)
def search(request: SearchRequest) -> SearchResponse:
    query = request.query.strip()
    if not query:
        return SearchResponse(results=[])

    try:
        vectors = run_embeddings([query])
    except AIUnavailableError:
        # Signal failure so the backend can fall back to keyword search.
        raise HTTPException(status_code=503, detail="search_unavailable")
    if not vectors:
        return SearchResponse(results=[])

    filters = request.filters
    rows = queries.vector_search(
        vectors[0],
        request.topK,
        category=filters.category if filters else None,
        language=filters.language if filters else None,
        published_after=filters.publishedAfter if filters else None,
    )
    return SearchResponse(results=[
        SearchHit(id=str(row["id"]), score=float(row["score"])) for row in rows
    ])
