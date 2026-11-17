"""Knowledge base for the AI assistant (RAG).

The Markdown guides are split into sections, turned into vectors with Gemini embeddings
and stored in Qdrant. For each question, the closest sections are found and given to
Gemini, so answers are based on our own documents.

Load the guides with:  uv run python -m solarvision_ai_service.knowledge
"""

import uuid
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Protocol

from google import genai
from google.genai import types
from qdrant_client import QdrantClient, models

from solarvision_ai_service.config import Settings, get_settings


@dataclass(frozen=True)
class Chunk:
    source: str  # file name, for example solar-maintenance-guide.md
    document: str  # the guide's title (its "# " heading)
    title: str  # the section's "## " heading
    text: str

    @property
    def point_id(self) -> str:
        # Same section -> same id, so loading the guides twice never creates duplicates
        return str(uuid.uuid5(uuid.NAMESPACE_URL, f"solarvision/{self.source}#{self.title}"))


@dataclass(frozen=True)
class SearchHit:
    chunk: Chunk
    score: float  # cosine similarity: closer to 1.0 means more similar


def split_markdown(source: str, markdown: str) -> list[Chunk]:
    """Splits a guide at its "## " headings: every section becomes one chunk."""
    document = source
    sections: list[tuple[str, list[str]]] = []
    for line in markdown.splitlines():
        if line.startswith("# "):
            document = line.removeprefix("# ").strip()
        elif line.startswith("## "):
            sections.append((line.removeprefix("## ").strip(), []))
        elif sections:
            sections[-1][1].append(line)

    chunks = []
    for title, lines in sections:
        text = "\n".join(lines).strip()
        if text:
            chunks.append(Chunk(source=source, document=document, title=title, text=text))
    return chunks


def load_chunks(folder: Path) -> list[Chunk]:
    chunks: list[Chunk] = []
    for path in sorted(folder.glob("*.md")):
        chunks.extend(split_markdown(path.name, path.read_text(encoding="utf-8")))
    return chunks


class Embedder(Protocol):
    def embed_document(self, chunk: Chunk) -> list[float]: ...

    def embed_query(self, question: str) -> list[float]: ...


class GeminiEmbedder:
    """Turns text into vectors with gemini-embedding-2.

    This model has no task_type setting, so we add the text prefixes Google recommends
    for question answering. Each text is sent on its own, so each gets its own vector.
    """

    def __init__(self, client: genai.Client, model: str, dimensions: int) -> None:
        self._client = client
        self._model = model
        self._dimensions = dimensions

    def embed_document(self, chunk: Chunk) -> list[float]:
        return self._embed(f"title: {chunk.document} - {chunk.title} | text: {chunk.text}")

    def embed_query(self, question: str) -> list[float]:
        return self._embed(f"task: question answering | query: {question}")

    def _embed(self, text: str) -> list[float]:
        result = self._client.models.embed_content(
            model=self._model,
            contents=text,
            config=types.EmbedContentConfig(output_dimensionality=self._dimensions),
        )
        return list(result.embeddings[0].values)


class KnowledgeBase:
    def __init__(
        self, qdrant: QdrantClient, embedder: Embedder, collection: str, dimensions: int
    ) -> None:
        self._qdrant = qdrant
        self._embedder = embedder
        self._collection = collection
        self._dimensions = dimensions

    def rebuild(self, chunks: list[Chunk]) -> int:
        """Replaces the whole collection with these chunks.

        All vectors are made first: if Gemini fails halfway, the old data stays untouched.
        """
        if not chunks:
            raise ValueError("No knowledge sections found: check the knowledge folder")
        vectors = [self._embedder.embed_document(chunk) for chunk in chunks]

        if self._qdrant.collection_exists(self._collection):
            self._qdrant.delete_collection(self._collection)
        self._qdrant.create_collection(
            self._collection,
            vectors_config=models.VectorParams(
                size=self._dimensions, distance=models.Distance.COSINE
            ),
        )
        self._qdrant.upsert(
            self._collection,
            points=[
                models.PointStruct(id=chunk.point_id, vector=vector, payload=asdict(chunk))
                for chunk, vector in zip(chunks, vectors, strict=True)
            ],
            wait=True,
        )
        return len(chunks)

    def search(self, question: str, limit: int = 4) -> list[SearchHit]:
        vector = self._embedder.embed_query(question)
        response = self._qdrant.query_points(
            self._collection, query=vector, limit=limit, with_payload=True
        )
        return [
            SearchHit(chunk=Chunk(**point.payload), score=point.score) for point in response.points
        ]


def create_knowledge_base(settings: Settings, gemini_client: genai.Client) -> KnowledgeBase:
    qdrant_key = settings.qdrant_api_key.get_secret_value() if settings.qdrant_api_key else None
    qdrant = QdrantClient(url=settings.qdrant_url, api_key=qdrant_key, timeout=10)
    embedder = GeminiEmbedder(
        gemini_client, settings.embedding_model, settings.embedding_dimensions
    )
    return KnowledgeBase(
        qdrant, embedder, settings.knowledge_collection, settings.embedding_dimensions
    )


def main() -> None:
    settings = get_settings()
    api_key = settings.gemini_api_key.get_secret_value().strip() if settings.gemini_api_key else ""
    if not api_key:
        raise SystemExit("GEMINI_API_KEY is missing in .env")

    chunks = load_chunks(settings.knowledge_dir)
    knowledge_base = create_knowledge_base(settings, genai.Client(api_key=api_key))
    count = knowledge_base.rebuild(chunks)
    print(f"Loaded {count} sections into the '{settings.knowledge_collection}' collection.")

    question = "My inverter gets very hot and its output drops"
    print(f"\nTest search: {question}")
    for hit in knowledge_base.search(question):
        print(f"  {hit.score:.3f}  {hit.chunk.source} > {hit.chunk.title}")


if __name__ == "__main__":
    main()
