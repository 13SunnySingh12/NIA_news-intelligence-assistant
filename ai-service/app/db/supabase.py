"""Postgres connection pool for the AI service (Supabase, with pgvector).

The pool is created lazily so importing the app never requires a live database
(useful for unit tests). Vectors are passed as string literals with an explicit
::vector cast, so no extra type registration is needed.
"""
from __future__ import annotations

from typing import Optional

from psycopg_pool import ConnectionPool

from app.config import settings

_pool: Optional[ConnectionPool] = None


def get_pool() -> ConnectionPool:
    global _pool
    if _pool is None:
        _pool = ConnectionPool(
            conninfo=settings.database_url,
            min_size=1,
            max_size=5,
            open=True,
            # Validate a connection before handing it out. Without this, a network
            # blip leaves dead connections in the pool and every later query fails
            # with "server closed the connection unexpectedly".
            check=ConnectionPool.check_connection,
            max_idle=300,
            kwargs={"connect_timeout": 10},
        )
    return _pool


def vector_literal(vector: list[float]) -> str:
    """Render a vector as the pgvector text form: [v1,v2,...]."""
    return "[" + ",".join(repr(float(v)) for v in vector) + "]"
