from types import SimpleNamespace

import pytest
from fastapi import HTTPException
from google.genai import types

from solarvision_ai_service.assistant import (
    FALLBACK_REPLY,
    Assistant,
    build_prompt,
    get_assistant,
)
from solarvision_ai_service.assistant_schemas import ChatRequest, ChatResponse
from solarvision_ai_service.config import Settings, get_settings
from solarvision_ai_service.knowledge import Chunk, SearchHit
from solarvision_ai_service.main import app

HEADERS = {"X-Internal-Api-Key": "test-key"}  # same key as API_KEY in conftest.py
URL = "/v1/assistant/chat"


# ---------- the HTTP endpoint (with a fake assistant) ----------


class FakeAssistant:
    def __init__(self) -> None:
        self.requests: list[ChatRequest] = []

    def chat(self, request: ChatRequest) -> ChatResponse:
        self.requests.append(request)
        return ChatResponse(reply="Clean the panels.", model="fake-model")


@pytest.fixture
def fake_assistant():
    fake = FakeAssistant()
    app.dependency_overrides[get_assistant] = lambda: fake
    yield fake
    app.dependency_overrides.pop(get_assistant, None)


def test_chat_returns_the_reply_and_passes_history(client, fake_assistant):
    body = {
        "message": "Why is my output low?",
        "history": [{"role": "user", "text": "Hi"}, {"role": "model", "text": "Hello!"}],
    }
    response = client.post(URL, headers=HEADERS, json=body)

    assert response.status_code == 200
    assert response.json() == {"reply": "Clean the panels.", "model": "fake-model", "sources": []}
    assert len(fake_assistant.requests[0].history) == 2


def test_blank_message_is_rejected(client, fake_assistant):
    response = client.post(URL, headers=HEADERS, json={"message": "   "})
    assert response.status_code == 422


def test_too_much_history_is_rejected(client, fake_assistant):
    history = [{"role": "user", "text": "hi"}] * 21
    response = client.post(URL, headers=HEADERS, json={"message": "Hi", "history": history})
    assert response.status_code == 422


def test_returns_503_when_no_gemini_key_is_configured(client):
    previous = app.dependency_overrides.get(get_settings)
    # _env_file=None: ignore the real .env, so this test never sees your real key
    app.dependency_overrides[get_settings] = lambda: Settings(
        internal_api_key="test-key", gemini_api_key=None, _env_file=None
    )
    try:
        response = client.post(URL, headers=HEADERS, json={"message": "Hi"})
    finally:
        if previous is None:
            app.dependency_overrides.pop(get_settings, None)
        else:
            app.dependency_overrides[get_settings] = previous

    assert response.status_code == 503


def test_gemini_models_setting_is_split_into_a_list():
    settings = Settings(internal_api_key="k", gemini_models=" model-a , model-b,", _env_file=None)
    assert settings.gemini_model_list == ["model-a", "model-b"]


# ---------- the Assistant itself (with a fake Gemini, no real requests) ----------


class FakeModels:
    """Stands in for client.models. Models listed in `failing` raise their error."""

    def __init__(self, text: str | None, failing: dict[str, Exception] | None = None) -> None:
        self.text = text
        self.failing = failing or {}
        self.calls: list[dict] = []

    def generate_content(self, **kwargs):
        self.calls.append(kwargs)
        error = self.failing.get(kwargs["model"])
        if error:
            raise error
        return SimpleNamespace(text=self.text)


class FakeKnowledgeBase:
    def __init__(self, hits: list[SearchHit] | None = None, error: Exception | None = None):
        self.hits = hits or []
        self.error = error

    def search(self, question: str, limit: int = 4) -> list[SearchHit]:
        if self.error:
            raise self.error
        return self.hits[:limit]


def make_assistant(
    text: str | None,
    knowledge_base=None,
    failing: dict[str, Exception] | None = None,
    model_names: list[str] | None = None,
) -> tuple[Assistant, FakeModels]:
    models = FakeModels(text, failing)
    assistant = Assistant(
        SimpleNamespace(models=models),
        model_names or ["test-model"],
        knowledge_base=knowledge_base,
    )
    return assistant, models


def hit(title: str, score: float) -> SearchHit:
    return SearchHit(Chunk("guide.md", "Guide", title, f"Text about {title}."), score)


def test_assistant_sends_history_then_the_new_question():
    assistant, models = make_assistant("Answer")
    request = ChatRequest(
        message="Second",
        history=[{"role": "user", "text": "First"}, {"role": "model", "text": "Reply"}],
    )

    response = assistant.chat(request)

    contents = models.calls[0]["contents"]
    assert [content.role for content in contents] == ["user", "model", "user"]
    assert contents[-1].parts[0].text == "Second"
    assert models.calls[0]["model"] == "test-model"
    assert response.reply == "Answer"
    assert response.model == "test-model"


def test_empty_gemini_answer_uses_the_fallback_reply():
    assistant, _ = make_assistant(None)
    assert assistant.chat(ChatRequest(message="Hi")).reply == FALLBACK_REPLY


def test_falls_back_to_the_next_model_when_the_first_is_too_slow():
    assistant, models = make_assistant(
        "Backup answer",
        failing={"primary": TimeoutError()},
        model_names=["primary", "backup"],
    )

    response = assistant.chat(ChatRequest(message="Hi"))

    assert [call["model"] for call in models.calls] == ["primary", "backup"]
    assert response.reply == "Backup answer"
    assert response.model == "backup"  # shows which model really answered


def test_returns_503_when_every_model_fails():
    assistant, models = make_assistant(
        "Never used",
        failing={"primary": TimeoutError(), "backup": TimeoutError()},
        model_names=["primary", "backup"],
    )

    with pytest.raises(HTTPException) as raised:
        assistant.chat(ChatRequest(message="Hi"))

    assert raised.value.status_code == 503
    assert len(models.calls) == 2


def test_an_assistant_needs_at_least_one_model():
    with pytest.raises(ValueError):
        Assistant(SimpleNamespace(models=FakeModels("x")), [])


def test_gemini_is_asked_with_the_configured_thinking_level():
    models = FakeModels("Answer")
    assistant = Assistant(SimpleNamespace(models=models), ["test-model"], thinking_level="high")

    assistant.chat(ChatRequest(message="Hi"))

    config = models.calls[0]["config"]
    assert config.thinking_config.thinking_level == types.ThinkingLevel.HIGH


def test_relevant_sections_are_given_to_gemini_and_returned_as_sources():
    knowledge_base = FakeKnowledgeBase([hit("Cooling", 0.82), hit("Unrelated", 0.41)])
    assistant, models = make_assistant("Check the fans [1].", knowledge_base)

    response = assistant.chat(ChatRequest(message="Inverter too hot?"))

    prompt = models.calls[0]["contents"][-1].parts[0].text
    assert "[1] Guide - Cooling" in prompt
    assert "Unrelated" not in prompt  # below the 0.6 minimum score
    assert prompt.endswith("Question: Inverter too hot?")
    assert [source.title for source in response.sources] == ["Cooling"]


def test_only_sections_cited_in_the_reply_are_returned_as_sources():
    knowledge_base = FakeKnowledgeBase([hit("Cooling", 0.82), hit("Cleaning", 0.75)])
    assistant, _ = make_assistant("Clean the modules gently [2].", knowledge_base)

    response = assistant.chat(ChatRequest(message="How do I clean panels?"))

    assert [source.title for source in response.sources] == ["Cleaning"]


def test_still_answers_when_the_knowledge_search_fails():
    knowledge_base = FakeKnowledgeBase(error=ConnectionError("Qdrant is down"))
    assistant, models = make_assistant("General answer", knowledge_base)

    response = assistant.chat(ChatRequest(message="Inverter too hot?"))

    assert models.calls[0]["contents"][-1].parts[0].text == "Inverter too hot?"
    assert response.reply == "General answer"
    assert response.sources == []


def test_build_prompt_without_sections_is_just_the_question():
    assert build_prompt("Hi", []) == "Hi"
