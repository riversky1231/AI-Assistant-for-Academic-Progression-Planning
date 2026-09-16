"""Server-side provider configuration; never accepted from a chat request."""
import os
from pathlib import Path

from dotenv import dotenv_values
from pydantic import BaseModel, Field, HttpUrl, SecretStr


class Settings(BaseModel):
    enabled: bool = False
    api_key: SecretStr = SecretStr("")
    base_url: HttpUrl = "https://api.deepseek.com"
    model: str = Field(default="deepseek-flash", min_length=1)
    timeout_seconds: float = Field(default=30, ge=1, le=120)
    max_rounds: int = Field(default=4, ge=1, le=8)


ENV_FILE = Path(__file__).resolve().parents[1] / ".env"


def get_settings() -> Settings:
    # Read non-secret settings from .env without modifying the process environment.
    values = dotenv_values(ENV_FILE, interpolate=False)
    names = {
        "enabled": "LLM_ENABLED",
        "base_url": "LLM_BASE_URL", "model": "LLM_MODEL",
        "timeout_seconds": "LLM_TIMEOUT_SECONDS", "max_rounds": "AGENT_MAX_ROUNDS",
    }
    return Settings(
        **{key: values[name] for key, name in names.items() if name in values},
        api_key=os.environ.get("LLM_API_KEY", ""),
    )
