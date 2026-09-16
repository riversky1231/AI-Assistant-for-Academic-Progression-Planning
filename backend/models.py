from __future__ import annotations

from typing import Literal
import re
from pydantic import BaseModel, ConfigDict, Field, field_validator


class SchoolSummary(BaseModel):
    id: int
    name: str
    province: str
    city: str
    level: str
    description: str


class Major(BaseModel):
    id: int
    name: str
    category: str
    description: str


class Admission(BaseModel):
    province: str
    year: int
    min_score: int
    min_rank: int
    subject_group: str
    source_name: str
    source_url: str
    major: Major


class SchoolDetail(SchoolSummary):
    admissions: list[Admission]


class RecommendationRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    province: str = Field(min_length=2, max_length=20)
    score: int = Field(ge=0, le=750)
    rank: int = Field(ge=1, le=10_000_000)
    major_preference: str | None = Field(default=None, max_length=50)
    region_preference: str | None = Field(default=None, max_length=50)

    @field_validator("major_preference", "region_preference")
    @classmethod
    def empty_string_to_none(cls, value: str | None) -> str | None:
        return value or None

    @field_validator("region_preference")
    @classmethod
    def validate_region(cls, value: str | None) -> str | None:
        if value and not any(part.strip() for part in re.split(r"[、,，/]+", value)):
            raise ValueError("地区偏好必须包含有效地区名称")
        return value


class RecommendedMajor(BaseModel):
    name: str
    min_score: int
    min_rank: int


class Recommendation(BaseModel):
    category: Literal["冲", "稳", "保"]
    gap: int
    score_gap: int
    match_gap: int
    school: SchoolSummary
    major: RecommendedMajor
    reason: str


class RecommendationResponse(BaseModel):
    message: str
    recommendations: dict[str, list[Recommendation]]
    disclaimer: str


class ChatMessage(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True, extra="forbid")
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=4000)


class ChatRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    message: str = Field(min_length=1, max_length=1000)
    context: str | None = Field(default=None, max_length=2000)
    history: list[ChatMessage] = Field(default_factory=list, max_length=20)


class ChatResponse(BaseModel):
    answer: str
    mode: Literal["rule_based", "llm_agent"]
    disclaimer: str
    tools_used: list[str] = Field(default_factory=list)
    fallback_reason: str | None = None
