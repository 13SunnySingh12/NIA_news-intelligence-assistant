from fastapi.testclient import TestClient

from app.main import app


def test_health_ok():
    response = TestClient(app).get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_ai_endpoints_require_internal_token():
    # Without the internal token, protected endpoints are rejected.
    response = TestClient(app).post("/ai/search", json={"query": "test", "topK": 5})
    assert response.status_code == 401


def test_health_accepts_head_for_uptime_monitors():
    """UptimeRobot and similar probe with HEAD. FastAPI's @app.get does not imply
    HEAD, so without an explicit route this returns 405 and the monitor reports a
    perfectly healthy service as down."""
    assert TestClient(app).head("/health").status_code == 200
