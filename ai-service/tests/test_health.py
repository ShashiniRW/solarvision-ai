import pytest
from fastapi.testclient import TestClient

from solarvision_ai_service.config import Settings, get_settings
from solarvision_ai_service.main import app


@pytest.fixture
def client():
    # tests use their own fake key, never the real one
    app.dependency_overrides[get_settings] = lambda: Settings(internal_api_key="test-key")
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


def test_health_is_public(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


def test_internal_endpoint_without_key_is_401(client):
    assert client.get("/v1/ping").status_code == 401


def test_internal_endpoint_with_wrong_key_is_401(client):
    response = client.get("/v1/ping", headers={"X-Internal-Api-Key": "wrong-key"})
    assert response.status_code == 401


def test_internal_endpoint_with_correct_key_works(client):
    response = client.get("/v1/ping", headers={"X-Internal-Api-Key": "test-key"})
    assert response.status_code == 200
    assert response.json() == {"message": "pong"}
