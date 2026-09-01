"""Request/response models. Field names match the Spring Boot JSON contract exactly."""
from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, Field


# ---- /ai/embed --------------------------------------------------------------
class EmbedItem(BaseModel):
    id: str
    text: str


class EmbedRequest(BaseModel):
    articles: list[EmbedItem]


class EmbedResponse(BaseModel):
    embedded: int


class EmbedPendingRequest(BaseModel):
    limit: int = 100


class EmbedPendingResponse(BaseModel):
    embedded: int
    remaining: int


# ---- /ai/search -------------------------------------------------------------
class SearchFilters(BaseModel):
    category: Optional[str] = None
    language: Optional[str] = None
    publishedAfter: Optional[str] = None


class SearchRequest(BaseModel):
    query: str
    topK: int = 8
    filters: Optional[SearchFilters] = None


class SearchHit(BaseModel):
    id: str
    score: float


class SearchResponse(BaseModel):
    results: list[SearchHit]


# ---- /ai/summarize ----------------------------------------------------------
class SummarizeRequest(BaseModel):
    articleId: str
    length: Literal["short", "detailed"] = "short"


class SummarizeResponse(BaseModel):
    text: str
    length: str


# ---- /ai/classify -----------------------------------------------------------
class ClassifyRequest(BaseModel):
    title: str = Field(min_length=1)
    description: Optional[str] = None


class ClassifyResponse(BaseModel):
    category: str


# ---- /ai/chat ---------------------------------------------------------------
class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    question: str = Field(min_length=1)
    conversation: Optional[list[ChatMessage]] = None


class ChatSource(BaseModel):
    id: str
    title: str
    source: str
    url: str


class ChatResponse(BaseModel):
    answer: str
    sources: list[ChatSource]
