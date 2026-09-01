"""Guard tests for the internal-token dependency.

The comparison is constant-time, which is easy to get subtly wrong: comparing
`str` values raises TypeError on non-ASCII input, and an unhandled TypeError
turns a hostile header into a 500 that is distinguishable from a clean 401.
"""
from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app

client = TestClient(app, raise_server_exceptions=False)

PROTECTED = "/ai/summarize"
BODY = {"article_id": "00000000-0000-0000-0000-000000000000", "length": "short"}


def _post(token: str | None):
    """Send the header as raw BYTES, the way curl or any hostile client does.

    Passing a str makes httpx encode it as ASCII and refuse non-ASCII locally,
    which would test the client rather than the server. Real traffic reaches the
    ASGI layer as bytes and is decoded there, so bytes is the honest input.
    """
    if token is None:
        headers = {}
    else:
        headers = {"X-Internal-Token": token.encode("utf-8")}
    return client.post(PROTECTED, json=BODY, headers=headers)


def test_missing_token_is_rejected():
    assert _post(None).status_code == 401


def test_empty_token_is_rejected():
    assert _post("").status_code == 401


def test_wrong_token_is_rejected():
    assert _post("definitely-not-the-token").status_code == 401


@pytest.mark.parametrize(
    "token",
    [
        "tökén-nön-ascii",          # accented latin
        "日本語トークン",              # non-latin script
        "emoji-token-\U0001f512",   # astral plane
        "\x00\x01\x02",             # control bytes
        "x" * 5000,                 # oversized
    ],
)
def test_hostile_tokens_are_rejected_cleanly(token):
    """Must be 401, never 500: a TypeError here would leak that the input was
    merely malformed rather than wrong, and hands an attacker a cheap error."""
    response = _post(token)
    assert response.status_code == 401, f"{token!r} produced {response.status_code}"
    assert response.json()["error"] == "unauthorized"


def test_correct_token_passes_the_guard():
    """The guard must let the real token through — it stops being a guard if it
    rejects everything. Anything past the guard is not a 401."""
    response = _post(settings.internal_token)
    assert response.status_code != 401
