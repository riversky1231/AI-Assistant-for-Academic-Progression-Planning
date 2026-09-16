# 升学规划智能助手：后端 MVP

本仓库当前实现 B 负责的 FastAPI + SQLite 后端。它包含 18 所院校、54 个专业及 54 条福建省 2025 年本地演示招生数据，用于课程项目联调，不代表真实招生数据。

## 启动

```powershell
python -m uvicorn backend.main:app --reload
```

启动后访问 `http://127.0.0.1:8000/docs` 查看 Swagger 文档。

## 接口

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/health` | 健康检查 |
| GET | `/schools` | 学校列表，可传 `keyword`、`province`、`limit` |
| GET | `/schools/{id}` | 学校、专业与录取数据详情 |
| POST | `/recommend` | 依据位次差返回冲、稳、保建议 |
| POST | `/chat` | 基础规则对话，供后续 Agent + LLM 接入替换 |

`/recommend` 请求示例：

```json
{
  "province": "福建",
  "score": 580,
  "rank": 15000,
  "major_preference": "计算机"
}
```

推荐结果只使用本地测试数据，且会明确标注“仅供参考”。

# AI-Assistant-for-Academic-Progression-Planning
