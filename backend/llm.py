"""Small Chat Completions adapter for DeepSeek and compatible providers."""
from typing import Literal

import httpx
from pydantic import BaseModel, Field, ValidationError

from .config import Settings


class AgentError(Exception):
    """A safe, stable error code. Provider bodies and credentials are not exposed."""


class FunctionCall(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    arguments: str = Field(max_length=10_000)


class ToolCall(BaseModel):
    id: str = Field(min_length=1, max_length=200)
    type: Literal["function"]
    function: FunctionCall


class AssistantMessage(BaseModel):
    role: Literal["assistant"]
    content: str | None = Field(default=None, max_length=16_000)
    tool_calls: list[ToolCall] | None = Field(default=None, max_length=8)
    # Some DeepSeek models require this field to be replayed during tool use.
    # It stays inside the agent loop and is never returned to the caller.
    reasoning_content: str | None = None


class ChatCompletionsClient:
    def __init__(self, settings: Settings, transport: httpx.BaseTransport | None = None):
        self.settings = settings
        self.transport = transport

    def complete(self, messages: list[dict], tools: list[dict]) -> AssistantMessage:
        try:
            with httpx.Client(timeout=self.settings.timeout_seconds, transport=self.transport) as client:
                response = client.post(
                    f"{str(self.settings.base_url).rstrip('/')}/chat/completions",
                    headers={"Authorization": f"Bearer {self.settings.api_key.get_secret_value()}"},
                    json={
                        "model": self.settings.model, "messages": messages,
                        "tools": tools, "tool_choice": "auto", "stream": False,
                        "max_tokens": 2048,
                    },
                )
                response.raise_for_status()
                choice = response.json()["choices"][0]
                if choice.get("finish_reason") not in ("stop", "tool_calls"):
                    raise AgentError("incomplete_response")
                return AssistantMessage.model_validate(choice["message"])
        except httpx.TimeoutException:
            raise AgentError("timeout") from None
        except httpx.HTTPStatusError as exc:
            code = exc.response.status_code
            reason = "authentication_failed" if code in (401, 403) else "rate_limited" if code == 429 else "provider_error"
            raise AgentError(reason) from None
        except httpx.RequestError:
            raise AgentError("connection_failed") from None
        except (ValueError, KeyError, IndexError, TypeError, ValidationError):
            raise AgentError("invalid_response") from None
