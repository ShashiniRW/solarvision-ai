from types import SimpleNamespace

import pytest

from solarvision_ai_service.assistant import FALLBACK_REPLY, Assistant, get_assistant
from solarvision_ai_service.assistant_schemas import ChatRequest, ChatResponse
from solarvision_ai_service.config import Settings, get_settings
from solarvision_ai_service.main import app

HEADERS = {"X-Internal-Api-Key": "test-key"}  # same key as API_KEY in conftest.py
URL = "/v1/assistant/chat"


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
    assert response.json() == {"reply": "Clean the panels.", "model": "fake-model"}
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


class FakeModels:
    """Stands in for client.models, so no real request is sent."""

    def __init__(self, text: str | None) -> None:
        self.text = text
        self.calls: list[dict] = []

    def generate_content(self, **kwargs):
        self.calls.append(kwargs)
        return SimpleNamespace(text=self.text)


def make_assistant(text: str | None) -> tuple[Assistant, FakeModels]:
    models = FakeModels(text)
    return Assistant(SimpleNamespace(models=models), "test-model"), models


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


def test_empty_gemini_answer_uses_the_fallback_reply():
    assistant, _ = make_assistant(None)
    assert assistant.chat(ChatRequest(message="Hi")).reply == FALLBACK_REPLY
