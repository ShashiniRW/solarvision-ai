from tests.conftest import API_KEY

HEADERS = {"X-Internal-Api-Key": API_KEY}


def week(actual_on_day_3: float) -> dict:
    days = [
        {"date": f"2026-11-0{day}", "expectedKwh": 40.0, "actualKwh": 32.0} for day in range(1, 8)
    ]
    days[2]["actualKwh"] = actual_on_day_3
    return {"days": days}


def test_endpoint_returns_camel_case_report(client):
    response = client.post("/v1/anomalies/performance", json=week(4.0), headers=HEADERS)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "OK"
    assert body["daysAnalysed"] == 7
    assert body["findings"][0]["type"] == "LOW_PERFORMANCE"
    assert body["findings"][0]["performanceRatio"] == 0.1


def test_endpoint_requires_the_internal_key(client):
    response = client.post("/v1/anomalies/performance", json=week(4.0))
    assert response.status_code == 401


def test_negative_energy_is_rejected(client):
    response = client.post("/v1/anomalies/performance", json=week(-1.0), headers=HEADERS)
    assert response.status_code == 422
