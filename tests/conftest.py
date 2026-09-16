import pytest

from backend import database, main
from backend.config import Settings


@pytest.fixture(autouse=True)
def isolated_backend(tmp_path, monkeypatch):
    """Tests never mutate the demo database or call a configured live provider."""
    monkeypatch.setattr(database, "DATABASE_PATH", tmp_path / "education.db")
    monkeypatch.setattr(main, "get_settings", lambda: Settings(enabled=False))
