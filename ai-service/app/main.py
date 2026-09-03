"""NIA AI service — FastAPI app wiring.

All /ai/* endpoints require the internal token. Errors are returned in NIA's
{error, message} shape; provider and model details never reach the client.
"""
from __future__ import annotations

import logging

from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.config import settings
from app.endpoints import chat, classify, embed, search, summarize
from app.security import verify_internal_token

logging.basicConfig(level=logging.INFO)
_log = logging.getLogger("nia")


def _validate_config() -> None:
    """Surface misconfiguration clearly at startup. Never logs secret values."""
    if settings.internal_token == "change-me":
        _log.warning("NIA_INTERNAL_TOKEN is unset (using the insecure default). "
                     "Set it in the root .env so Spring Boot and the AI service share one secret.")
    if "localhost" in settings.database_url:
        _log.warning("DATABASE_URL points at localhost - set it to your Supabase connection string "
                     "for semantic search, summaries, and embeddings to work.")
    if not settings.gemini_api_key and not settings.groq_api_key:
        _log.warning("No LLM API key configured - the assistant and summaries will report unavailable "
                     "until GEMINI_API_KEY (recommended) or GROQ_API_KEY is set.")


_validate_config()

app = FastAPI(title="NIA AI Service", version="1.0.0")

_ERROR_MESSAGES = {
    "unauthorized": "Unauthorized.",
    "article_not_found": "That article couldn't be found.",
    "assistant_unavailable": "The AI assistant is unavailable right now — please try again in a few minutes.",
    "summary_unavailable": "Couldn't generate a summary right now — please try again in a few minutes.",
    "search_unavailable": "Search is unavailable right now.",
    "classification_unavailable": "Couldn't classify this article right now — please try again shortly.",
}


@app.api_route("/health", methods=["GET", "HEAD"])
def health() -> dict:
    """Liveness probe.

    HEAD is accepted as well as GET: uptime monitors commonly probe with HEAD,
    and FastAPI's @app.get does not imply it (unlike Spring MVC, which adds HEAD
    to @GetMapping automatically). Without this the service answers 405 and a
    monitor reports a healthy container as down.
    """
    return {"status": "ok", "service": "nia-ai-service"}


# Internal, token-protected AI endpoints.
app.include_router(embed.router, prefix="/ai", dependencies=[Depends(verify_internal_token)])
app.include_router(search.router, prefix="/ai", dependencies=[Depends(verify_internal_token)])
app.include_router(summarize.router, prefix="/ai", dependencies=[Depends(verify_internal_token)])
app.include_router(classify.router, prefix="/ai", dependencies=[Depends(verify_internal_token)])
app.include_router(chat.router, prefix="/ai", dependencies=[Depends(verify_internal_token)])


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException) -> JSONResponse:
    code = exc.detail if isinstance(exc.detail, str) else "error"
    message = _ERROR_MESSAGES.get(code, "Request could not be completed.")
    return JSONResponse(status_code=exc.status_code, content={"error": code, "message": message})


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_: Request, __: RequestValidationError) -> JSONResponse:
    return JSONResponse(
        status_code=400,
        content={"error": "invalid_input", "message": "That request looked malformed — please check and try again."},
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception) -> JSONResponse:
    logging.getLogger("nia").error("Unhandled error: %s", type(exc).__name__)
    return JSONResponse(
        status_code=500,
        content={"error": "server_error", "message": "Something went wrong on our side. Please try again."},
    )
