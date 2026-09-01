import pytest
from fastapi.testclient import TestClient

from app.endpoints.classify import _match_category
from app.main import app
from app.config import settings

client = TestClient(app)
HEADERS = {"X-Internal-Token": settings.internal_token}


@pytest.mark.parametrize("reply,expected", [
    ("technology", "technology"),
    ("  Sports  ", "sports"),
    ("This is about politics.", "politics"),
    ("gibberish", "world"),          # unknown -> safe default
    ("", "world"),
])
def test_match_category_maps_onto_nia_categories(reply, expected):
    assert _match_category(reply) == expected


def test_classify_endpoint_returns_category(monkeypatch):
    monkeypatch.setattr("app.endpoints.classify.task_router.run", lambda *a, **k: "technology")
    response = client.post("/ai/classify",
                           json={"title": "New AI chip released", "description": "Faster inference"},
                           headers=HEADERS)
    assert response.status_code == 200
    assert response.json()["category"] == "technology"


def test_classify_requires_internal_token():
    response = client.post("/ai/classify", json={"title": "x"})
    assert response.status_code == 401
