import json
import asyncio

import httpx

import main
from recognition import MODEL_NAME, recognize


class FakeEngine:
    def embedding(self, image_bytes: bytes) -> list[float]:
        assert image_bytes == b"image-data"
        return [1.0] + [0.0] * 127


def setup_module():
    main.app.dependency_overrides[main.ready_engine] = lambda: FakeEngine()


def teardown_module():
    main.app.dependency_overrides.clear()


def request(method: str, path: str, **kwargs):
    async def send():
        transport = httpx.ASGITransport(app=main.app)
        async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
            return await client.request(method, path, **kwargs)
    return asyncio.run(send())


def test_health(monkeypatch):
    monkeypatch.setattr(main, "get_engine", lambda: FakeEngine())
    response = request("GET", "/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "modelName": MODEL_NAME}


def test_register_requires_service_token(monkeypatch):
    monkeypatch.setenv("AI_SERVICE_TOKEN", "secret")
    response = request("POST",
        "/face/register", files={"image": ("face.jpg", b"image-data", "image/jpeg")}
    )
    assert response.status_code == 401


def test_register_returns_embedding(monkeypatch):
    monkeypatch.setenv("AI_SERVICE_TOKEN", "secret")
    response = request("POST",
        "/face/register",
        headers={"X-AI-Service-Token": "secret"},
        files={"image": ("face.jpg", b"image-data", "image/jpeg")},
    )
    assert response.status_code == 200
    assert response.json()["modelName"] == MODEL_NAME
    assert len(response.json()["embedding"]) == 128


def test_recognize_matches_only_supplied_candidates(monkeypatch):
    monkeypatch.setenv("AI_SERVICE_TOKEN", "secret")
    candidates = [{"studentId": 101, "modelName": MODEL_NAME,
                   "embedding": [1.0] + [0.0] * 127}]
    response = request("POST",
        "/face/recognize",
        headers={"X-AI-Service-Token": "secret"},
        data={"candidates": json.dumps(candidates)},
        files={"image": ("frame.jpg", b"image-data", "image/jpeg")},
    )
    assert response.status_code == 200
    assert response.json()["studentId"] == 101


def test_recognize_rejects_bad_candidate(monkeypatch):
    monkeypatch.setenv("AI_SERVICE_TOKEN", "secret")
    response = request("POST",
        "/face/recognize",
        headers={"X-AI-Service-Token": "secret"},
        data={"candidates": '[{"studentId":101,"embedding":[]}]'},
        files={"image": ("frame.jpg", b"image-data", "image/jpeg")},
    )
    assert response.status_code == 422


def test_recognize_rejects_ambiguous_match():
    embedding = [1.0] + [0.0] * 127
    candidates = [
        {"studentId": 101, "modelName": MODEL_NAME, "embedding": embedding},
        {"studentId": 102, "modelName": MODEL_NAME, "embedding": embedding},
    ]
    assert recognize(embedding, candidates)["studentId"] is None
