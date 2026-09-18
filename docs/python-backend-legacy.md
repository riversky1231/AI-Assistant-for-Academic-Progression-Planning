# Python 后端与 Agent 历史说明

本文保留合并前的使用说明。当前主入口见 [项目 README](../README.md)，Java Agent 契约见 [Agent 接口文档](Agent接口文档.md)。以下命令按仓库根目录执行。Python Agent 继续读取原始 Skill，原文件现保存在 `backend/skills/zhangxuefeng-skill/UPSTREAM-SKILL.md`；同目录 `SKILL.md` 为 Java 项目的适配入口。

## 当前版本：Java 后端 + 微信小程序

当前可运行主后端是 `backend/pom.xml` 对应的 Spring Boot 工程，默认端口 **8080**，使用 MySQL、Redis 和账号 Token 鉴权。下方 Python / Agent 说明为历史资料，与当前 Java 路由不完全一致。

已新增原生微信小程序：包含首页、院校库、院校详情、志愿推荐、冲稳保结果、我的、账号登录。直接用微信开发者工具导入仓库根目录即可；配置与验收见 [小程序使用说明](../miniprogram/README.md)，实际 Java 接口见 [前端接口文档](frontend-api.md)。

Java 后端启动：先按 `backend/src/main/resources/application.yml` 配置 MySQL 和 Redis，创建 `academic_planning` 数据库，然后在 `backend` 目录执行：

```powershell
mvn spring-boot:run
```

程序从 `../sql/schema.sql` 和 `../sql/data.sql` 初始化结构与演示数据。已有数据库请先确认初始化脚本适合当前数据。Swagger 为 `http://127.0.0.1:8080/swagger-ui.html`。前端默认连接 `http://127.0.0.1:8080`，部署地址在 `miniprogram/config/api.js` 中配置。

前端检查（仓库根目录）：

```powershell
node --test tests/miniprogram.test.cjs
python tests/check_miniprogram_markup.py
```

## 历史版本：Python 后端与 Agent MVP

本仓库实现 FastAPI + SQLite 后端，以及支持 DeepSeek / OpenAI 兼容 Chat Completions 接口的工具调用 Agent。包含本地演示招生数据，以及少量可追溯的真实招生记录，用于课程项目联调。推荐结果仅供参考，报考前须以省级招考机构和院校当年公布的信息为准。

当前真实记录包括天津财经大学珠江学院发布的《2025年普通本科分专业录取分数线一览表》中的福建省物理类 6 个专业，以及福建农林大学的 7 个普通本科专业和 2 个中外合作办学专业。通过 `GET /schools/{id}` 可查看每条记录的科类、来源名称和原始链接；其余未标注来源的记录仍为本地演示数据。

## 2. 启动与测试

```powershell
python -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
.venv\Scripts\python.exe -m uvicorn backend.main:app --reload
```

启动后访问 `http://127.0.0.1:8000/docs` 查看 Swagger 文档。

前端联调请参阅 [前端接口文档](frontend-api.md)。

## 3. 接口

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/health` | 健康检查 |
| GET | `/schools` | 学校列表，可传 `keyword`、`province`、`limit` |
| GET | `/schools/{id}` | 学校、专业与录取数据详情 |
| POST | `/recommend` | 结合分数差与位次差返回冲、稳、保建议 |
| POST | `/chat` | LLM 工具调用对话；未启用或调用失败时返回基础规则提示 |

`/recommend` 请求示例：

```json
{
  "province": "福建",
  "subject_type": "物理类",
  "score": 580,
  "rank": 15000,
  "major_preference": "计算机",
  "region_preference": "江浙沪"
}
```

推荐结果使用本地数据；其中包含演示记录和少量带来源标注的真实录取记录，结果会明确标注“仅供参考”。
地区偏好支持单个省市、`江浙沪`、`长三角`，以及使用顿号、逗号或斜杠分隔的多个地区。

## 启用 DeepSeek / 兼容服务

普通配置放在项目根目录 `.env`（首次可复制 `.env.example`）：

```dotenv
LLM_ENABLED=true
LLM_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-flash
LLM_TIMEOUT_SECONDS=30
AGENT_MAX_ROUNDS=4
```

API Key 只从系统或进程环境变量 `LLM_API_KEY` 读取，不从 `.env` 读取。在启动服务的 PowerShell 窗口中设置：

```powershell
# 隐藏输入密钥，避免将密钥直接写入命令历史
$llmSecret = Read-Host "LLM API Key" -AsSecureString
$env:LLM_API_KEY = [System.Net.NetworkCredential]::new("", $llmSecret).Password
Remove-Variable llmSecret
.venv\Scripts\python.exe -m uvicorn backend.main:app --reload
```

除密钥外的设置仅从 `.env` 读取，未填写时使用代码默认值，不接受环境变量覆盖。不启用时不发送 LLM 请求。`.env` 已被 Git 忽略，请不要在其中保存密钥。
上面的密钥设置仅对当前 PowerShell 及其启动的子进程有效。也可在 Windows 用户环境变量中配置 `LLM_API_KEY`，配置后重新打开终端再启动服务。
其他兼容服务只需修改地址、密钥和模型名；地址填写 API 根路径，例如服务要求 `/v1` 时需包含 `/v1`，不要填写 `/chat/completions`。
模型必须支持 Chat Completions 的 `tools` / `tool_calls`。不同供应商的扩展参数仍可能需要适配。

默认模型名与接口示例依据 [DeepSeek 官方快速开始](https://api-docs.deepseek.com/)；工具协议参见 [DeepSeek Tool Calls](https://api-docs.deepseek.com/guides/tool_calls/) 和 [OpenAI Function calling](https://developers.openai.com/api/docs/guides/function-calling)。

## Agent 对话与前端联调

继续调用 `POST /chat`，原有 `message` 和 `context` 保持兼容：

```json
{
  "message": "想学计算机，帮我推荐江浙沪的院校",
  "context": "希望毕业后在长三角工作",
  "history": [
    {"role": "user", "content": "我是福建考生，580分，位次15000"},
    {"role": "assistant", "content": "你偏好什么专业和地区？"}
  ]
}
```

Agent 的流程为：用户消息与历史 → LLM 选择工具 → 后端校验并执行 → 结果回传 LLM → 自然语言回答。

Agent 每次运行只加载项目内 `backend/skills/zhangxuefeng-skill` 原始且未修改的 `UPSTREAM-SKILL.md`，并在其后附加本项目的工具边界与默认直言风格。研究资料和示例保留为原始文件，由 `read_skill_resource` 工具按需读取，避免把整套材料塞进每次 LLM 请求。图片等二进制资源也原样保留在项目中，但当前 Chat Completions 接口只能发送文本。当前 Agent 不能联网，仍只依据本地工具数据回答具体招生问题。

| 工具 | 功能 |
|---|---|
| `search_schools` | 按关键词、学校所在省份查询院校 |
| `get_school_detail` | 根据院校 ID 查询专业与招生演示记录 |
| `recommend_schools` | 复用 `/recommend` 的分数、位次和地区筛选规则 |
| `read_skill_resource` | 按需读取项目内张雪峰技能包的研究资料或示例 |

工具只读，不允许模型执行任意 SQL、代码或自定义工具。提示词要求缺少生源省份、分数、位次时先追问，并根据工具结果解释推荐。

成功响应示例（回答内容会随模型变化）：

```json
{
  "answer": "已根据本地模拟数据整理建议，仅供参考。",
  "mode": "llm_agent",
  "disclaimer": "本地招生数据含演示记录和少量来源标注的真实记录，模型回答仅供参考，不承诺录取结果；请核对院校官方信息。",
  "tools_used": ["recommend_schools"],
  "fallback_reason": null
}
```

- `history` 由前端保存并随请求发送，最多 20 条，每条最多 4000 字符，只接受 `user` / `assistant`。历史不要重复包含当前 `message`。服务端不保存会话，不支持刷新后的自动恢复。
- `context` 是可选补充背景，最多 2000 字符；当前消息最多 1000 字符。
- `tools_used` 是本轮成功执行的工具名称，不包含模型内部推理。
- LLM 关闭、缺少密钥或调用失败时，HTTP 仍返回 200，`mode=rule_based`；前端应根据 `mode` 和 `fallback_reason` 显示降级状态。
- 常见降级原因：`disabled`、`missing_api_key`、`invalid_configuration`、`authentication_failed`、`rate_limited`、`timeout`、`connection_failed`、`provider_error`、`invalid_response`、`incomplete_response`、`empty_response`、`round_limit`。
- 默认最多 4 次 LLM 请求，每次最多返回 8 个工具调用；单次网络超时默认 30 秒，不自动重试。整段对话可能需要多个请求耗时。

## 开发与测试

```powershell
.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.venv\Scripts\python.exe -m pytest -q
```

测试使用临时 SQLite 数据库和模拟 HTTP Provider，不读取真实密钥发起调用，也不修改演示数据库。

```text
backend/main.py      API 与规则降级，复用原查询和推荐逻辑
backend/models.py    请求与响应模型
backend/config.py    环境变量密钥与 .env 普通配置
backend/llm.py       Chat Completions HTTP 适配与响应校验
backend/agent.py     提示词、工具注册、参数校验与有限轮次执行
backend/database.py SQLite 与演示数据初始化
```

当前范围：同步非流式对话、前端传入历史、只读本地工具，以及少量来源标注的真实招生记录。尚无联网检索、向量检索或持久化会话。提示词不能保证模型完全不产生错误，回答中的具体信息仍需核对工具和官方数据。启用 LLM 后，消息、历史、补充背景及查询结果会发送至所配置的模型服务。
