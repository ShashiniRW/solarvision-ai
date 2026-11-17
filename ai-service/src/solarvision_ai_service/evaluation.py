"""Measures the assistant's accuracy and speed on fixed questions with known answers.

Run:  uv run python -m solarvision_ai_service.evaluation
It uses the real Gemini API (about 2 requests per question) and the local Qdrant.
"""

import json
import time
from dataclasses import dataclass
from pathlib import Path

from fastapi import HTTPException
from qdrant_client import QdrantClient

from solarvision_ai_service.assistant import build_assistant
from solarvision_ai_service.assistant_schemas import ChatRequest
from solarvision_ai_service.config import get_settings

QUESTIONS_FILE = Path(__file__).resolve().parents[2] / "evaluation" / "questions.json"
PAUSE_SECONDS = 2  # stays well below the free limit of 15 requests per minute


@dataclass(frozen=True)
class Case:
    question: str
    must_include: list[str]  # every entry must appear; "a|b" means a or b
    source: str | None  # guide section the answer must cite; None = must cite nothing


@dataclass(frozen=True)
class Outcome:
    passed: bool
    problems: list[str]


def load_cases(path: Path = QUESTIONS_FILE) -> list[Case]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return [
        Case(item["question"], item.get("must_include", []), item.get("source")) for item in data
    ]


def score(case: Case, reply: str, source_titles: list[str]) -> Outcome:
    """Checks one answer: the key facts are there, and the right section is cited."""
    text = reply.lower()
    problems = [
        f"missing '{expected}'"
        for expected in case.must_include
        if not any(option.lower() in text for option in expected.split("|"))
    ]
    if case.source is None:
        if source_titles:
            problems.append(f"cited {source_titles} but should cite nothing")
    elif case.source not in source_titles:
        problems.append(f"did not cite '{case.source}'")
    return Outcome(passed=not problems, problems=problems)


def check_knowledge_base_ready() -> None:
    """Stops early if Qdrant is down or the guides are not loaded: the numbers would be wrong."""
    settings = get_settings()
    qdrant_key = settings.qdrant_api_key.get_secret_value() if settings.qdrant_api_key else None
    qdrant = QdrantClient(url=settings.qdrant_url, api_key=qdrant_key, timeout=5)
    try:
        points = qdrant.count(settings.knowledge_collection).count
    except Exception as error:
        raise SystemExit(
            f"Qdrant is not ready at {settings.qdrant_url} ({type(error).__name__}).\n"
            "Start Docker Desktop, wait for 'Engine running', and load the guides with:\n"
            "  uv run python -m solarvision_ai_service.knowledge"
        ) from error
    if points == 0:
        raise SystemExit("The knowledge base is empty. Load the guides first.")


def main() -> None:
    settings = get_settings()
    api_key = settings.gemini_api_key.get_secret_value().strip() if settings.gemini_api_key else ""
    if not api_key:
        raise SystemExit("GEMINI_API_KEY is missing in .env")

    check_knowledge_base_ready()

    assistant = build_assistant(api_key)
    cases = load_cases()
    print(f"Models: {settings.gemini_models}   Thinking: {settings.gemini_thinking_level}\n")

    passed = 0
    times: list[float] = []
    for number, case in enumerate(cases, start=1):
        started = time.perf_counter()
        try:
            response = assistant.chat(ChatRequest(message=case.question))
        except HTTPException as error:
            outcome = Outcome(False, [f"error {error.status_code}: {error.detail}"])
            model = "-"
        else:
            outcome = score(case, response.reply, [source.title for source in response.sources])
            model = response.model
        seconds = time.perf_counter() - started

        times.append(seconds)
        passed += outcome.passed
        result = "PASS" if outcome.passed else "FAIL"
        print(f"{number:>2}  {result}  {seconds:5.1f} s  {model:<24} {case.question}")
        for problem in outcome.problems:
            print(f"                -> {problem}")
        time.sleep(PAUSE_SECONDS)

    print(
        f"\nAccuracy: {passed}/{len(cases)} ({passed / len(cases):.0%})"
        f"   Average: {sum(times) / len(times):.1f} s   Slowest: {max(times):.1f} s"
    )


if __name__ == "__main__":
    main()
