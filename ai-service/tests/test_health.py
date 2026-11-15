from tests.conftest import API_KEY


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
    response = client.get("/v1/ping", headers={"X-Internal-Api-Key": API_KEY})
    assert response.status_code == 200
    assert response.json() == {"message": "pong"}
