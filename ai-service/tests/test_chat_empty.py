from fastapi.testclient import TestClient

from app import router as router_module
from app.config import settings
from app.main import app
from app.rag import retrieve as retrieve_module

# Use the actually-configured internal token so the test is independent of env.
HEADERS = {"X-Internal-Token": settings.internal_token}


def test_empty_retrieval_short_circuits_without_calling_llm(monkeypatch):
    # No relevant articles retrieved.
    monkeypatch.setattr(retrieve_module, "retrieve", lambda *args, **kwargs: [])

    # The LLM must NOT be called on empty retrieval.
    def fail_if_called(*args, **kwargs):
        raise AssertionError("chat model should not be called when retrieval is empty")

    monkeypatch.setattr(router_module.task_router, "run", fail_if_called)

    response = TestClient(app).post("/ai/chat", json={"question": "anything"}, headers=HEADERS)

    assert response.status_code == 200
    body = response.json()
    assert body["answer"].startswith("I don't have any relevant articles")
    assert body["sources"] == []
