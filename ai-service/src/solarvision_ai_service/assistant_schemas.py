from typing import Annotated, Literal

from pydantic import Field, StringConstraints

from solarvision_ai_service.schemas import CamelModel

# Strips spaces first, so a message of only spaces is rejected (422)
MessageText = Annotated[
    str, StringConstraints(strip_whitespace=True, min_length=1, max_length=2000)
]
HistoryText = Annotated[
    str, StringConstraints(strip_whitespace=True, min_length=1, max_length=4000)
]


class ChatMessage(CamelModel):
    """One earlier turn of the conversation. Gemini calls the assistant's role 'model'."""

    role: Literal["user", "model"]
    text: HistoryText


class ChatRequest(CamelModel):
    message: MessageText
    # Limit the history so one request cannot use up the free quota
    history: list[ChatMessage] = Field(default_factory=list, max_length=20)


class Source(CamelModel):
    """A guide section the answer was based on, shown under the answer in the chat."""

    document: str
    title: str


class ChatResponse(CamelModel):
    reply: str
    model: str
    sources: list[Source] = Field(default_factory=list)
