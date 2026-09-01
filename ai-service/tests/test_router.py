import httpx
import pytest

from app.config import settings
from app.router import AIUnavailableError, TaskRouter, is_transient


def _http_error(status: int) -> httpx.HTTPStatusError:
    request = httpx.Request("POST", "http://test")
    response = httpx.Response(status, request=request)
    return httpx.HTTPStatusError("err", request=request, response=response)


class _FakeProvider:
    def __init__(self, name, result=None, error=None):
        self.name = name
        self._result = result
        self._error = error
        self.calls = 0

    def available(self) -> bool:
        return True

    def chat(self, messages, system=None):
        self.calls += 1
        if self._error:
            raise self._error
        return self._result


def _router(providers):
    router = TaskRouter()
    router._providers = providers
    return router


def test_chat_routes_to_gemini():
    gemini = _FakeProvider("GEMINI", result="from gemini")
    groq = _FakeProvider("GROQ", result="from groq")
    fallback = _FakeProvider("OPENROUTER", result="from openrouter")
    router = _router({"GEMINI": gemini, "GROQ": groq, "OPENROUTER": fallback})

    assert router.run("chat", [{"role": "user", "content": "hi"}]) == "from gemini"
    assert fallback.calls == 0  # primary succeeded; fallback untouched


def test_summary_and_classify_route_to_groq():
    gemini = _FakeProvider("GEMINI", result="from gemini")
    groq = _FakeProvider("GROQ", result="from groq")
    fallback = _FakeProvider("OPENROUTER", result="from openrouter")
    router = _router({"GEMINI": gemini, "GROQ": groq, "OPENROUTER": fallback})

    assert router.run("summary", [{"role": "user", "content": "hi"}]) == "from groq"
    assert router.run("classify", [{"role": "user", "content": "hi"}]) == "from groq"


def test_falls_back_to_openrouter_on_transient_error():
    gemini = _FakeProvider("GEMINI", error=httpx.TimeoutException("timeout"))
    fallback = _FakeProvider("OPENROUTER", result="from openrouter")
    router = _router({"GEMINI": gemini, "OPENROUTER": fallback})

    assert router.run("chat", [{"role": "user", "content": "hi"}]) == "from openrouter"
    assert fallback.calls == 1


def test_does_not_fall_back_on_client_error():
    # A 400 means our request was malformed — switching providers must NOT happen.
    gemini = _FakeProvider("GEMINI", error=_http_error(400))
    fallback = _FakeProvider("OPENROUTER", result="from openrouter")
    router = _router({"GEMINI": gemini, "OPENROUTER": fallback})

    with pytest.raises(httpx.HTTPStatusError):
        router.run("chat", [{"role": "user", "content": "hi"}])
    assert fallback.calls == 0


def test_raises_when_no_provider_available():
    with pytest.raises(AIUnavailableError):
        _router({}).run("chat", [{"role": "user", "content": "hi"}])


def test_is_transient_classification():
    assert is_transient(httpx.TimeoutException("t")) is True
    assert is_transient(_http_error(429)) is True    # rate limit -> fall back
    assert is_transient(_http_error(503)) is True    # outage -> fall back
    assert is_transient(_http_error(404)) is True    # model not found -> another provider may work
    assert is_transient(_http_error(400)) is False   # bad request -> no fall back
    assert is_transient(_http_error(422)) is False   # unprocessable -> no fall back


def test_walks_the_whole_fallback_chain(monkeypatch):
    """A chain longer than one hop is walked in order until a provider succeeds."""
    monkeypatch.setattr(settings, "ai_chat_provider", "GEMINI")
    monkeypatch.setattr(settings, "ai_fallback_provider", "GROQ,OPENROUTER")

    gemini = _FakeProvider("GEMINI", error=_http_error(429))
    groq = _FakeProvider("GROQ", error=_http_error(503))
    openrouter = _FakeProvider("OPENROUTER", result="from openrouter")
    router = _router({"GEMINI": gemini, "GROQ": groq, "OPENROUTER": openrouter})

    assert router.run("chat", [{"role": "user", "content": "hi"}]) == "from openrouter"
    assert (gemini.calls, groq.calls, openrouter.calls) == (1, 1, 1)


def test_raises_when_every_provider_in_chain_fails(monkeypatch):
    monkeypatch.setattr(settings, "ai_chat_provider", "GEMINI")
    monkeypatch.setattr(settings, "ai_fallback_provider", "GROQ")

    gemini = _FakeProvider("GEMINI", error=_http_error(429))
    groq = _FakeProvider("GROQ", error=_http_error(500))
    router = _router({"GEMINI": gemini, "GROQ": groq})

    with pytest.raises(AIUnavailableError):
        router.run("chat", [{"role": "user", "content": "hi"}])
