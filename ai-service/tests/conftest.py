import pytest
from fastapi.testclient import TestClient

from solarvision_ai_service.config import Settings, get_settings
from solarvision_ai_service.main import app

API_KEY = "test-key"


@pytest.fixture
def client():
    # tests use their own fake key, never the real one
    app.dependency_overrides[get_settings] = lambda: Settings(internal_api_key=API_KEY)
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()
