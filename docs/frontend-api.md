# 前端接口文档

后端默认地址：`http://127.0.0.1:8000`。启动后可访问：

- Swagger UI：`/docs`
- OpenAPI JSON：`/openapi.json`

所有请求和响应均使用 JSON，除 `GET /health` 与 `GET /schools` 外，请求头应包含：

```http
Content-Type: application/json
```

## 微信小程序接入

小程序使用 `wx.request`，不受浏览器 CORS 限制；后端的 CORS 设置不会决定小程序能否请求。

- 真机和体验版必须使用已备案的 HTTPS 域名，并在微信公众平台的“开发管理 → 开发设置 → 服务器域名”添加该域名到 `request 合法域名`。
- `127.0.0.1`、`localhost` 和局域网 HTTP 地址只适合开发者工具调试。开发阶段可在开发者工具中勾选“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”。不要依赖这个开关发布体验版或正式版。
- 建议在 `miniprogram/config/api.ts` 集中维护地址，并为开发和生产环境分别配置：

```ts
export const API_BASE = "https://api.example.com";
// 本地开发时可临时使用："http://127.0.0.1:8000"
```

后端仍允许本机浏览器端口跨域，便于 Swagger、管理页或 H5 调试；这不影响小程序。

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

## 推荐的小程序调用封装

```ts
import { API_BASE } from "../config/api";

export function request<T>(path: string, method: "GET" | "POST", data?: object): Promise<T> {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${API_BASE}${path}`,
      method,
      data,
      header: { "content-type": "application/json" },
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as T);
          return;
        }
        reject({ statusCode: response.statusCode, data: response.data });
      },
      fail(error) {
        reject({ type: "network_error", error });
      },
    });
  });
}

export const getHealth = () => request<{ status: string }>("/health", "GET");
export const getSchools = (data?: { keyword?: string; province?: string; limit?: number }) =>
  request("/schools", "GET", data);
export const recommend = (data: {
  province: string; score: number; rank: number;
  major_preference?: string; region_preference?: string;
}) => request("/recommend", "POST", data);
export const chat = (data: { message: string; context?: string; history?: object[] }) =>
  request("/chat", "POST", data);
```

页面加载时可请求 `GET /health`；网络异常、非 `2xx` 响应和 `422` 分开处理。对 `/chat` 的 `rule_based` 响应不要按网络错误处理。
