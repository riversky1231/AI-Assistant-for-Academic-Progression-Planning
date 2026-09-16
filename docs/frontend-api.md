# 前端接口文档

后端默认地址：`http://127.0.0.1:8000`。启动后可访问：

- Swagger UI：`/docs`
- OpenAPI JSON：`/openapi.json`

所有请求和响应均使用 JSON，除 `GET /health` 与 `GET /schools` 外，请求头应包含：

```http
Content-Type: application/json
```

开发环境允许 `http://localhost:<任意端口>` 和 `http://127.0.0.1:<任意端口>` 跨域访问。生产环境需在后端单独收紧 CORS 来源。

## 1. 健康检查

`GET /health`

```json
{"status": "ok"}
```

前端可在应用初始化或服务重试时使用此接口判断后端是否可达。

## 2. 院校列表

`GET /schools`

| 查询参数 | 类型 | 必填 | 说明 |
|---|---:|:---:|---|
| `keyword` | string | 否 | 院校名称或简介关键词，1–50 个字符 |
| `province` | string | 否 | 学校所在地省份，2–20 个字符；不是考生生源地 |
| `limit` | number | 否 | 返回条数，默认 `20`，范围 `1–50` |

示例：`GET /schools?province=福建&keyword=农林&limit=10`

成功响应 `200`：

以下为字段结构示例，数值会随请求条件和本地数据变化：

```json
[
  {
    "id": 20,
    "name": "福建农林大学",
    "province": "福建",
    "city": "福州",
    "level": "省重点",
    "description": "以农林科学、生命科学为优势特色的省属重点高校。"
  }
]
```

没有匹配项时返回 `200` 和空数组 `[]`，前端应显示“暂无匹配院校”，不要将其当成服务错误。

## 3. 院校与录取详情

`GET /schools/{school_id}`

`school_id` 为院校列表中返回的正整数 `id`。不要根据数据库顺序猜测 ID。

成功响应 `200`：

```json
{
  "id": 20,
  "name": "福建农林大学",
  "province": "福建",
  "city": "福州",
  "level": "省重点",
  "description": "以农林科学、生命科学为优势特色的省属重点高校。",
  "admissions": [
    {
      "province": "福建",
      "year": 2025,
      "min_score": 573,
      "min_rank": 23443,
      "subject_group": "物理类（再选化学）",
      "source_name": "果然优志：2025年福建农林大学在福建高考录取投档分数线及位次",
      "source_url": "https://www.hzgrys.net/score/35/2025/1136.html",
      "major": {
        "id": 61,
        "name": "计算机科学与技术",
        "category": "工学",
        "description": "学习计算机系统、软件与算法基础。"
      }
    }
  ]
}
```

字段说明：

| 字段 | 含义 |
|---|---|
| `province` | 此条录取数据对应的考生生源省份 |
| `subject_group` | 选科或科类要求；历史演示数据可能为“未区分科类” |
| `source_name` / `source_url` | 数据来源。`source_name=本地演示数据` 或空链接时，前端应标注为演示数据 |
| `min_score` / `min_rank` | 历史最低分和最低位次，不是录取承诺 |

不存在的 `school_id` 返回 `404`：

```json
{"detail": "未找到该院校"}
```

## 4. 冲稳保推荐

`POST /recommend`

请求体：

```json
{
  "province": "福建",
  "score": 580,
  "rank": 15000,
  "major_preference": "计算机",
  "region_preference": "江浙沪"
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---:|:---:|---|
| `province` | string | 是 | 考生生源省份，2–20 个字符 |
| `score` | integer | 是 | `0–750` |
| `rank` | integer | 是 | `1–10000000`，数值越小位次越靠前 |
| `major_preference` | string/null | 否 | 专业名称关键词，最多 50 个字符 |
| `region_preference` | string/null | 否 | 学校所在地；可使用 `江浙沪`、`长三角`，或用 `、`、`,`、`，`、`/` 分隔多个地区 |

成功响应 `200`：

```json
{
  "message": "已结合分数与位次生成冲、稳、保建议。",
  "recommendations": {
    "冲": [],
    "稳": [
      {
        "category": "稳",
        "gap": 500,
        "score_gap": 3,
        "match_gap": 800,
        "school": {"id": 2, "name": "福州大学", "province": "福建", "city": "福州", "level": "211", "description": "国家双一流建设高校。"},
        "major": {"name": "计算机科学与技术", "min_score": 577, "min_rank": 15500},
        "reason": "历史最低分 577、最低位次 15500；您的分数差 +3 分、位次差 +500 名，综合判定为“稳”。"
      }
    ],
    "保": []
  },
  "disclaimer": "推荐基于本地演示数据和少量来源标注的录取记录，仅供参考，不承诺录取结果。"
}
```

`recommendations` 始终包含 `冲`、`稳`、`保` 三个数组。某个数组为空是正常结果；`message` 为“暂无符合条件的数据。”时，三个数组都会为空。

前端可用 `match_gap` 做同一组内排序展示。它是后端综合分数差和位次差的内部匹配值，不应作为真实录取概率展示。

参数不合法时返回 `422`，格式由 FastAPI 标准校验错误决定：

```json
{
  "detail": [
    {"type": "greater_than_equal", "loc": ["body", "rank"], "msg": "Input should be greater than or equal to 1", "input": 0}
  ]
}
```

## 5. 智能对话

`POST /chat`

请求体：

```json
{
  "message": "我在福建高考，580 分，位次 15000，想学计算机，推荐江浙沪院校。",
  "context": "希望毕业后在长三角工作",
  "history": [
    {"role": "user", "content": "我更在意就业。"},
    {"role": "assistant", "content": "请提供生源省份、分数和位次。"}
  ]
}
```

| 字段 | 类型 | 必填 | 约束 |
|---|---:|:---:|---|
| `message` | string | 是 | 当前消息，1–1000 个字符 |
| `context` | string/null | 否 | 补充背景，最多 2000 个字符 |
| `history` | array | 否 | 最多 20 条，仅允许 `user` 与 `assistant` 角色；每条内容最多 4000 个字符 |

前端负责保存 `history` 并随每次请求发送；后端不保存会话。不要把当前 `message` 再放进 `history`。

成功响应：

```json
{
  "answer": "先别急着给自己贴‘江浙沪计算机’的标签。位次、专业和地区都要同时匹配，以下建议基于本地记录。",
  "mode": "llm_agent",
  "disclaimer": "本地数据包含演示数据与少量来源记录，模型回答仅供参考，不承诺录取结果；请核对院校官方信息。",
  "tools_used": ["recommend_schools"],
  "fallback_reason": null
}
```

`mode` 决定前端状态：

| `mode` | 含义 | 前端处理 |
|---|---|---|
| `llm_agent` | LLM Agent 已正常完成 | 正常展示 `answer` 和 `tools_used` |
| `rule_based` | LLM 未启用或本轮不可用，已安全降级 | 展示 `answer`，并以非阻塞提示告知基础模式 |

当 `mode=rule_based` 时，读取 `fallback_reason`：

| 值 | 含义 |
|---|---|
| `disabled` | 后端未启用 LLM |
| `missing_api_key` | 后端缺少 LLM 密钥 |
| `invalid_configuration` | LLM 配置无效 |
| `authentication_failed` | 密钥或供应商认证失败 |
| `rate_limited` | 供应商限流 |
| `timeout` / `connection_failed` | 网络或供应商连接问题 |
| `provider_error` | 供应商服务错误 |
| `invalid_response` / `incomplete_response` / `empty_response` | 供应商返回无法使用的结果 |
| `round_limit` | 工具调用超过本轮上限 |

以上降级仍返回 `200`，因为接口已经给出可展示的基础回答。只有请求体不合法时才返回 `422`。

## 推荐的前端调用封装

```ts
const API_BASE = import.meta.env.VITE_API_BASE ?? "http://127.0.0.1:8000";

export async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await response.json();
  if (!response.ok) throw data;
  return data as T;
}
```

页面加载时可请求 `GET /health`；网络异常、非 `200` 响应和 `422` 分开处理。对 `/chat` 的 `rule_based` 响应不要按网络错误处理。
