from fastapi.testclient import TestClient

from backend.main import app


def test_health_and_openapi() -> None:
    with TestClient(app) as client:
        assert client.get("/health").json() == {"status": "ok"}
        assert client.get("/openapi.json").status_code == 200


def test_school_list_and_detail() -> None:
    with TestClient(app) as client:
        schools = client.get("/schools", params={"province": "福建"})
        assert schools.status_code == 200
        assert len(schools.json()) == 5
        assert len(client.get("/schools").json()) == 18
        detail = client.get(f"/schools/{schools.json()[0]['id']}")
        assert detail.status_code == 200
        assert len(detail.json()["admissions"]) == 3


def test_expanded_seed_data_is_idempotent() -> None:
    with TestClient(app):
        pass
    with TestClient(app):
        pass
    from backend.database import get_connection

    with get_connection() as connection:
        counts = {
            table: connection.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            for table in ("school", "major", "admission")
        }
    assert counts == {"school": 18, "major": 54, "admission": 54}


def test_recommendation_contains_reach_match_and_safety() -> None:
    with TestClient(app) as client:
        response = client.post(
            "/recommend",
            json={"province": "福建", "score": 580, "rank": 15000, "major_preference": "计算机"},
        )
    assert response.status_code == 200
    groups = response.json()["recommendations"]
    assert groups["冲"]
    assert groups["稳"]
    assert groups["保"]


def test_invalid_input_missing_school_and_no_data() -> None:
    with TestClient(app) as client:
        invalid = client.post("/recommend", json={"province": "福建", "score": 900, "rank": 0})
        missing = client.get("/schools/9999")
        no_data = client.post("/recommend", json={"province": "海南", "score": 580, "rank": 15000})
    assert invalid.status_code == 422
    assert missing.status_code == 404
    assert no_data.status_code == 200
    assert no_data.json()["message"] == "暂无符合条件的数据。"


def test_search_uses_parameterized_query() -> None:
    with TestClient(app) as client:
        response = client.get("/schools", params={"keyword": "' OR 1=1 --"})
    assert response.status_code == 200
    assert response.json() == []


def test_chat_rejects_empty_messages() -> None:
    with TestClient(app) as client:
        assert client.post("/chat", json={"message": ""}).status_code == 422
        assert client.post("/chat", json={"message": "软件工程可以考人工智能吗？"}).status_code == 200
