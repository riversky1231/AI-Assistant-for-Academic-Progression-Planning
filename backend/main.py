from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Literal

from fastapi import FastAPI, HTTPException, Path, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ConfigDict, Field, field_validator

from .database import get_connection, initialize_database


@asynccontextmanager
async def lifespan(_: FastAPI):
    initialize_database()
    yield


app = FastAPI(
    title="升学规划智能助手 API",
    version="0.1.0",
    description="MVP 后端：本地院校数据查询与规则推荐。所有推荐仅供参考。",
    lifespan=lifespan,
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost", "http://127.0.0.1"],
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)


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


class RecommendedMajor(BaseModel):
    name: str
    min_score: int
    min_rank: int


class Recommendation(BaseModel):
    category: Literal["冲", "稳", "保"]
    gap: int
    school: SchoolSummary
    major: RecommendedMajor
    reason: str


class RecommendationResponse(BaseModel):
    message: str
    recommendations: dict[str, list[Recommendation]]
    disclaimer: str


class ChatRequest(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)
    message: str = Field(min_length=1, max_length=1000)
    context: str | None = Field(default=None, max_length=2000)


class ChatResponse(BaseModel):
    answer: str
    mode: Literal["rule_based"]
    disclaimer: str


@app.get("/health", tags=["system"])
def health_check() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/schools", response_model=list[SchoolSummary], tags=["schools"])
def list_schools(
    keyword: str | None = Query(default=None, min_length=1, max_length=50),
    province: str | None = Query(default=None, min_length=2, max_length=20),
    limit: int = Query(default=20, ge=1, le=50),
) -> list[SchoolSummary]:
    conditions: list[str] = []
    parameters: list[object] = []
    if keyword:
        conditions.append("(name LIKE ? OR description LIKE ?)")
        parameters.extend([f"%{keyword}%", f"%{keyword}%"])
    if province:
        conditions.append("province = ?")
        parameters.append(province)
    where_clause = f" WHERE {' AND '.join(conditions)}" if conditions else ""
    with get_connection() as connection:
        rows = connection.execute(
            f"SELECT id, name, province, city, level, description FROM school{where_clause} "
            "ORDER BY id LIMIT ?",
            [*parameters, limit],
        ).fetchall()
    return [SchoolSummary.model_validate(dict(row)) for row in rows]


@app.get("/schools/{school_id}", response_model=SchoolDetail, tags=["schools"])
def get_school(school_id: int = Path(ge=1)) -> SchoolDetail:
    with get_connection() as connection:
        school = connection.execute(
            "SELECT id, name, province, city, level, description FROM school WHERE id = ?", (school_id,)
        ).fetchone()
        if school is None:
            raise HTTPException(status_code=404, detail="未找到该院校")
        rows = connection.execute(
            """SELECT a.province, a.year, a.min_score, a.min_rank,
                      m.id AS major_id, m.name AS major_name, m.category, m.description
               FROM admission a JOIN major m ON m.id = a.major_id
               WHERE a.school_id = ? ORDER BY a.year DESC, m.name""",
            (school_id,),
        ).fetchall()
    admissions = [
        Admission(
            province=row["province"], year=row["year"], min_score=row["min_score"], min_rank=row["min_rank"],
            major=Major(id=row["major_id"], name=row["major_name"], category=row["category"], description=row["description"]),
        )
        for row in rows
    ]
    return SchoolDetail(**dict(school), admissions=admissions)


def _category_for_gap(gap: int) -> Literal["冲", "稳", "保"]:
    if gap < -2_000:
        return "冲"
    if gap > 2_000:
        return "保"
    return "稳"


@app.post("/recommend", response_model=RecommendationResponse, tags=["recommendation"])
def recommend(request: RecommendationRequest) -> RecommendationResponse:
    conditions = ["a.province = ?"]
    parameters: list[object] = [request.province]
    if request.major_preference:
        conditions.append("m.name LIKE ?")
        parameters.append(f"%{request.major_preference}%")
    if request.region_preference:
        conditions.append("(s.province LIKE ? OR s.city LIKE ?)")
        parameters.extend([f"%{request.region_preference}%", f"%{request.region_preference}%"])
    where_clause = " AND ".join(conditions)
    with get_connection() as connection:
        rows = connection.execute(
            f"""SELECT s.id, s.name, s.province, s.city, s.level, s.description,
                       m.name AS major_name, a.min_score, a.min_rank
                FROM admission a
                JOIN school s ON s.id = a.school_id
                JOIN major m ON m.id = a.major_id
                WHERE {where_clause} AND a.year = (
                    SELECT MAX(a2.year) FROM admission a2
                    WHERE a2.school_id = a.school_id AND a2.major_id = a.major_id AND a2.province = a.province
                )""",
            parameters,
        ).fetchall()
    grouped: dict[str, list[Recommendation]] = {"冲": [], "稳": [], "保": []}
    for row in rows:
        gap = row["min_rank"] - request.rank
        category = _category_for_gap(gap)
        reason = f"历史最低位次 {row['min_rank']}，与您的位次相差 {abs(gap)} 名。"
        grouped[category].append(
            Recommendation(
                category=category, gap=gap,
                school=SchoolSummary(**{key: row[key] for key in SchoolSummary.model_fields}),
                major=RecommendedMajor(name=row["major_name"], min_score=row["min_score"], min_rank=row["min_rank"]),
                reason=reason,
            )
        )
    for category in grouped:
        grouped[category].sort(key=lambda item: abs(item.gap))
        grouped[category] = grouped[category][:5]
    has_results = any(grouped.values())
    return RecommendationResponse(
        message="已按位次差生成冲、稳、保建议。" if has_results else "暂无符合条件的数据。",
        recommendations=grouped,
        disclaimer="推荐基于本地测试数据与规则，仅供参考，不承诺录取结果。",
    )


@app.post("/chat", response_model=ChatResponse, tags=["chat"])
def chat(request: ChatRequest) -> ChatResponse:
    message = request.message
    if any(word in message for word in ("分数", "位次", "推荐", "学校", "专业")):
        answer = "可先填写省份、分数、位次和专业偏好，再调用 /recommend 获取本地数据推荐。具体录取数据以 /schools 返回内容为准。"
    elif any(word in message for word in ("考研", "人工智能", "软件工程")):
        answer = "软件工程可报考人工智能等相关方向；建议结合目标院校招生目录、初试科目和个人基础制定复习计划。当前后端未接入实时招生政策。"
    else:
        answer = "这是基础规则对话服务。C 组接入 Agent + LLM 后，可在保留本地事实数据约束的前提下扩展回答能力。"
    return ChatResponse(answer=answer, mode="rule_based", disclaimer="信息仅供参考；数据不足时请以院校官方招生信息为准。")
