from fastapi import APIRouter, Depends, FastAPI

from solarvision_ai_service.assistant import AssistantDep
from solarvision_ai_service.assistant_schemas import ChatRequest, ChatResponse
from solarvision_ai_service.performance import analyse_performance
from solarvision_ai_service.schemas import PerformanceReport, PerformanceRequest
from solarvision_ai_service.security import require_internal_api_key

app = FastAPI(
    title="SolarVision AI Service",
    version="0.1.0",
    description=(
        "Anomaly detection and AI assistant for SolarVision AI. Called only by the backend."
    ),
)


@app.get("/health", tags=["health"])
def health() -> dict[str, str]:
    """Public: lets Docker and the hosting platform check that the service is running."""
    return {"status": "UP"}


# Every /v1 endpoint is internal: only the Spring backend (which knows the key) may call it
v1 = APIRouter(prefix="/v1", dependencies=[Depends(require_internal_api_key)])


@v1.get("/ping", tags=["internal"])
def ping() -> dict[str, str]:
    """Lets the backend check that its key works."""
    return {"message": "pong"}


@v1.post("/anomalies/performance", tags=["anomalies"])
def detect_performance_anomalies(request: PerformanceRequest) -> PerformanceReport:
    """Finds days where actual energy is far below expected energy."""
    return analyse_performance(request.days)


@v1.post("/assistant/chat", tags=["assistant"])
def chat(request: ChatRequest, assistant: AssistantDep) -> ChatResponse:
    """Answers a question about solar maintenance or SolarVision, using the earlier turns."""
    return assistant.chat(request)


app.include_router(v1)
