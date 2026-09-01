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
