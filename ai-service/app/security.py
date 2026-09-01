"""Internal-token guard. FastAPI is only ever called by Spring Boot."""
from __future__ import annotations

import hmac

from fastapi import Header, HTTPException, status

from app.config import settings


async def verify_internal_token(x_internal_token: str | None = Header(default=None)) -> None:
    """Reject any request that doesn't carry the shared internal secret.

    The comparison is constant-time: a plain `!=` returns as soon as two bytes
    differ, which leaks the secret one character at a time to anyone who can time
    the responses. This service is reachable over the network in the hosted
    deployment, so that is a real exposure, not a theoretical one.

    Both sides are compared as BYTES. `hmac.compare_digest` raises TypeError on
    `str` arguments containing non-ASCII characters, and header values can carry
    any bytes an attacker chooses — comparing strings turned a hostile header into
    an unhandled 500 instead of a clean 401.
    """
    if not x_internal_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="unauthorized",
        )
    provided = x_internal_token.encode("utf-8", "replace")
    expected = settings.internal_token.encode("utf-8", "replace")
    if not hmac.compare_digest(provided, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="unauthorized",
        )
