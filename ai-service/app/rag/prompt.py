"""Prompt construction for RAG chat and on-demand summaries."""
from __future__ import annotations

from typing import Optional

CHAT_SYSTEM_PROMPT = (
    "You are NIA, a news assistant. Answer using ONLY the numbered articles provided "
    "below. If the articles do not contain enough information, say so explicitly. Do not "
    "invent facts. Do not invent URLs or sources. When you use an article, cite it inline "
    "with its bracket number, like [1] or [2]. Keep answers concise and neutral."
)

SUMMARY_SYSTEM_PROMPT = (
    "You summarize news articles neutrally and concisely. Do not add facts that are not in "
    "the article. If the article is missing key details, say so."
)


def _excerpt(article: dict, limit: int = 300) -> str:
    text = article.get("description") or article.get("content") or ""
    text = " ".join(text.split())
    return text[:limit]


def _date(article: dict) -> str:
    published = article.get("published_at")
    try:
        return published.date().isoformat() if published else "unknown date"
    except AttributeError:
        return str(published)


def build_context(articles: list[dict]) -> str:
    """Numbered context block; the numbers are what the model cites as [n]."""
    lines = []
    for index, article in enumerate(articles, start=1):
        lines.append(
            f"[{index}] {article.get('title', '')} — {article.get('source', '')} "
            f"({_date(article)}) — {article.get('url', '')} — {_excerpt(article)}"
        )
    return "\n".join(lines)


def build_chat_messages(question: str, context: str,
                        conversation: Optional[list[dict]] = None) -> list[dict]:
    """Short prior turns for continuity, then the grounded question with its context."""
    messages: list[dict] = []
    if conversation:
        for turn in conversation[-6:]:
            role = turn.get("role", "user")
            content = turn.get("content", "")
            if content:
                messages.append({"role": role, "content": content})
    messages.append({
        "role": "user",
        "content": f"Question: {question}\n\nArticles:\n{context}",
    })
    return messages


def build_summary_user(title: str, source: str, body: str, sentences: int) -> str:
    return (
        f"Summarize the following article in {sentences} sentences.\n"
        f"Title: {title}\n"
        f"Source: {source}\n"
        f"Content:\n{body}"
    )
