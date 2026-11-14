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
