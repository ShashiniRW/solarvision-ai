# SolarVision AI Service

Python service for anomaly detection and the AI assistant. It is **internal**: only the
Spring Boot backend calls it, using a shared key in the `X-Internal-Api-Key` header.

**Stack:** Python 3.14 · FastAPI · Pydantic Settings · uv · pytest · Ruff

## Run locally

```bash
cp .env.example .env        # then set INTERNAL_API_KEY to a long random value
uv sync
uv run fastapi dev src/solarvision_ai_service/main.py
```

- Health check: <http://127.0.0.1:8000/health>
- API docs: <http://127.0.0.1:8000/docs>

## Checks

```bash
uv run ruff check .
uv run ruff format --check .
uv run pytest
```

## Configuration

| Variable           | Required | Description                                        |
|--------------------|----------|----------------------------------------------------|
| `INTERNAL_API_KEY` | yes      | Shared secret; the backend must use the same value |

## AI assistant (RAG)

The assistant answers questions about solar maintenance and SolarVision. It searches the
guides in `knowledge/` (stored as vectors in Qdrant), gives the closest sections to Gemini,
and returns the answer with the sources it cited.

### Start Qdrant (once)

```powershell
docker run -d --name solarvision-qdrant --restart unless-stopped -p 127.0.0.1:6333:6333 -v solarvision_qdrant:/qdrant/storage qdrant/qdrant:v1.20.0
```

Qdrant listens on `127.0.0.1` only, because it has no password by default.

### Load the guides

Run this again whenever a file in `knowledge/` changes:

```powershell
uv run python -m solarvision_ai_service.knowledge
```

### Settings

| Variable                | Default                                       | Meaning                                         |
| ----------------------- | --------------------------------------------- | ----------------------------------------------- |
| `GEMINI_API_KEY`        | none                                          | Required for chat; without it, chat returns 503 |
| `GEMINI_MODELS`         | `gemini-3.5-flash-lite,gemini-3.1-flash-lite` | Tried in order when a model is busy or slow     |
| `GEMINI_THINKING_LEVEL` | `low`                                         | `minimal`, `low`, `medium` or `high`            |
| `QDRANT_URL`            | `http://localhost:6333`                       | Address of Qdrant                               |
| `RAG_TOP_K`             | `4`                                           | Guide sections given to Gemini                  |
| `RAG_MIN_SCORE`         | `0.6`                                         | Less similar sections are ignored               |

### Design decisions

- **Flash Lite models:** on the free tier they allow 500 requests per day each (larger
  models allow only 20), and they answer in a few seconds. The fallback model has its own
  limit, so the demo gets about 1,000 answers per day.
- **Graceful degradation:** if Qdrant is down, the assistant still answers, without
  sources. If every model is busy, the backend returns a clear 503.
- **Privacy:** on the Gemini free tier, Google may use requests to improve its products,
  so only demo data is sent. Real customer data would require the paid tier.
