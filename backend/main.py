from __future__ import annotations

from contextlib import asynccontextmanager
import re
from typing import Literal
import logging

from fastapi import FastAPI, HTTPException, Path, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import ValidationError

from .agent import build_tools, run_agent
from .config import get_settings
from .llm import AgentError, ChatCompletionsClient
from .models import (SchoolSummary, Major, Admission, SchoolDetail, RecommendationRequest, RecommendedMajor, Recommendation, RecommendationResponse, ChatRequest, ChatResponse)

from .database import get_connection, initialize_database


REGION_ALIASES = {
    "江浙沪": ("江苏", "浙江", "上海"),
    "长三角": ("江苏", "浙江", "上海"),
}
SCORE_TO_RANK_WEIGHT = 100


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
    allow_origin_regex=r"^http://(localhost|127\.0\.0\.1)(:\d+)?$",
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)


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
                      a.subject_group, a.source_name, a.source_url,
                      m.id AS major_id, m.name AS major_name, m.category, m.description
               FROM admission a JOIN major m ON m.id = a.major_id
               WHERE a.school_id = ? ORDER BY a.year DESC, m.name""",
            (school_id,),
        ).fetchall()
    admissions = [
        Admission(
            province=row["province"], year=row["year"], min_score=row["min_score"], min_rank=row["min_rank"],
            subject_group=row["subject_group"], source_name=row["source_name"], source_url=row["source_url"],
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


def _expand_region_preference(value: str) -> list[str]:
    normalized = value.replace(" ", "")
    if normalized in REGION_ALIASES:
        return list(REGION_ALIASES[normalized])
    parts = [part for part in re.split(r"[、,，/]+", normalized) if part]
    return list(dict.fromkeys(parts))


@app.post("/recommend", response_model=RecommendationResponse, tags=["recommendation"])
def recommend(request: RecommendationRequest) -> RecommendationResponse:
    conditions = ["a.province = ?"]
    parameters: list[object] = [request.province]
    if request.major_preference:
        conditions.append("m.name LIKE ?")
        parameters.append(f"%{request.major_preference}%")
    if request.region_preference:
        regions = _expand_region_preference(request.region_preference)
        region_conditions = ["(s.province LIKE ? OR s.city LIKE ?)" for _ in regions]
        conditions.append(f"({' OR '.join(region_conditions)})")
        for region in regions:
            parameters.extend([f"%{region}%", f"%{region}%"])
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
        rank_gap = row["min_rank"] - request.rank
        score_gap = request.score - row["min_score"]
        match_gap = rank_gap + score_gap * SCORE_TO_RANK_WEIGHT
        category = _category_for_gap(match_gap)
        reason = (
            f"历史最低分 {row['min_score']}、最低位次 {row['min_rank']}；"
            f"您的分数差 {score_gap:+d} 分、位次差 {rank_gap:+d} 名，综合判定为“{category}”。"
        )
        grouped[category].append(
            Recommendation(
                category=category,
                gap=rank_gap,
                score_gap=score_gap,
                match_gap=match_gap,
                school=SchoolSummary(**{key: row[key] for key in SchoolSummary.model_fields}),
                major=RecommendedMajor(name=row["major_name"], min_score=row["min_score"], min_rank=row["min_rank"]),
                reason=reason,
            )
        )
    for category in grouped:
        grouped[category].sort(key=lambda item: abs(item.match_gap))
        grouped[category] = grouped[category][:5]
    has_results = any(grouped.values())
    return RecommendationResponse(
        message="已结合分数与位次生成冲、稳、保建议。" if has_results else "暂无符合条件的数据。",
        recommendations=grouped,
        disclaimer="推荐基于本地演示数据和少量来源标注的录取记录，仅供参考，不承诺录取结果。",
    )


@app.post("/chat", response_model=ChatResponse, tags=["chat"])
def chat(request: ChatRequest) -> ChatResponse:
    try:
        settings = get_settings()
    except ValidationError:
        return rule_based_chat(request, "invalid_configuration")
    if not settings.enabled:
        return rule_based_chat(request, "disabled")
    if not settings.api_key.get_secret_value().strip():
        return rule_based_chat(request, "missing_api_key")
    try:
        answer, used = run_agent(
            request, ChatCompletionsClient(settings),
            build_tools(list_schools, get_school, recommend), settings.max_rounds,
        )
        return ChatResponse(
            answer=answer, mode="llm_agent", tools_used=used,
            disclaimer="本地数据包含演示数据与少量来源记录，模型回答仅供参考，不承诺录取结果；请核对院校官方信息。",
        )
    except AgentError as exc:
        # Only log a controlled code, never provider bodies or user messages.
        reason = str(exc)
        logging.getLogger(__name__).warning("LLM agent fallback: %s", reason)
        return rule_based_chat(request, reason)


def rule_based_chat(request: ChatRequest, reason: str) -> ChatResponse:
    message = request.message
    if any(word in message for word in ("分数", "位次", "推荐", "学校", "专业")):
        answer = "可先填写省份、分数、位次和专业偏好，再调用 /recommend 获取本地数据推荐。具体录取数据以 /schools 返回内容为准。"
    elif any(word in message for word in ("考研", "人工智能", "软件工程")):
        answer = "软件工程可报考人工智能等相关方向；建议结合目标院校招生目录、初试科目和个人基础制定复习计划。当前后端未接入实时招生政策。"
    else:
        answer = "当前为基础规则对话服务。你可以查询院校、专业与冲稳保建议；启用 LLM 后支持自然语言工具调用和多轮对话。"
    if reason != "disabled":
        answer = "智能对话暂不可用，以下为基础规则提示。" + answer
    return ChatResponse(
        answer=answer,
        mode="rule_based",
        fallback_reason=reason,
        disclaimer="信息仅供参考；数据不足时请以院校官方招生信息为准。",
    )
