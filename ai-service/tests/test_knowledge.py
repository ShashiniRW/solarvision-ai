from pathlib import Path

import pytest
from qdrant_client import QdrantClient

from solarvision_ai_service.knowledge import Chunk, KnowledgeBase, load_chunks, split_markdown

KNOWLEDGE_DIR = Path(__file__).parent.parent / "knowledge"

GUIDE = """# Test Guide

Intro text before the first section is ignored.

## Cooling

The inverter is too hot.

### Fans

Check the fans.

## Cleaning

Clean the modules.

## Empty section
"""


def test_split_markdown_makes_one_chunk_per_section():
    chunks = split_markdown("test.md", GUIDE)

    assert [chunk.title for chunk in chunks] == ["Cooling", "Cleaning"]
    assert chunks[0].document == "Test Guide"
    assert "### Fans" in chunks[0].text  # smaller headings stay inside their section
    assert chunks[1].text == "Clean the modules."


def test_chunk_ids_are_stable_and_unique():
    first = split_markdown("test.md", GUIDE)
    second = split_markdown("test.md", GUIDE)

    assert [chunk.point_id for chunk in first] == [chunk.point_id for chunk in second]
    assert len({chunk.point_id for chunk in first}) == len(first)


def test_the_real_guides_are_valid():
    chunks = load_chunks(KNOWLEDGE_DIR)

    assert len(chunks) >= 10
    assert all(chunk.text for chunk in chunks)
    # Unique section titles per guide, so no section overwrites another in Qdrant
    assert len({chunk.point_id for chunk in chunks}) == len(chunks)


class FakeEmbedder:
    """Tiny 3-number vectors chosen by keyword, so tests need no Gemini key."""

    @staticmethod
    def _vector(text: str) -> list[float]:
        text = text.lower()
        if "hot" in text:
            return [1.0, 0.0, 0.0]
        if "clean" in text:
            return [0.0, 1.0, 0.0]
        return [0.0, 0.0, 1.0]

    def embed_document(self, chunk: Chunk) -> list[float]:
        return self._vector(chunk.text)

    def embed_query(self, question: str) -> list[float]:
        return self._vector(question)


def make_knowledge_base() -> KnowledgeBase:
    return KnowledgeBase(QdrantClient(":memory:"), FakeEmbedder(), "test", dimensions=3)


def test_search_returns_the_closest_section():
    knowledge_base = make_knowledge_base()
    knowledge_base.rebuild(split_markdown("test.md", GUIDE))

    hits = knowledge_base.search("Why is my inverter so hot?", limit=1)

    assert hits[0].chunk.title == "Cooling"
    assert hits[0].score == pytest.approx(1.0)


def test_rebuild_replaces_the_old_content():
    knowledge_base = make_knowledge_base()
    knowledge_base.rebuild(split_markdown("test.md", GUIDE))
    knowledge_base.rebuild([Chunk("other.md", "Other", "Cleaning tips", "Clean gently.")])

    hits = knowledge_base.search("clean", limit=5)

    assert [hit.chunk.source for hit in hits] == ["other.md"]
