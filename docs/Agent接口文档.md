# Agent 咨询接口文档

更新日期：2026-09-18。面向前端、Apifox/Postman 联调与后端维护，描述当前实现，不把规划功能当作已实现能力。

## 1. 接口概览

| 项目 | 约定 |
| --- | --- |
| 方法与路径 | `POST /agent/chat` |
| 本地默认地址 | `http://127.0.0.1:8080`，端口可由 `SERVER_PORT` 覆盖 |
| 请求与响应 | UTF-8 JSON；`Content-Type: application/json` |
| 认证 | 请求头 `satoken: <token_value>`，直接填写 token，不加 `Bearer` |
| 接口权限 | `recommend:use` |
| 院校查询权限 | 模型调用 `search_schools` 或 `school_detail` 时另需 `school:read` |
| 响应方式 | 非流式；一次请求完成后返回完整 JSON，不支持 SSE |
| 咨询框架 | 每轮固定注入张雪峰视角，无需触发词 |
| 在线文档 | `/swagger-ui.html`；OpenAPI 描述 `/v3/api-docs` |

对外的 Agent HTTP 接口目前只有 `/agent/chat`。四个内部工具由模型选择、后端校验执行，不是四个新增的 Agent HTTP 路由。

## 2. 认证准备

先调用公开接口 `POST /auth/login`，Body 为 JSON：

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

这是数据库初始化提供的演示账号，仅在账号未改动时适用。登录成功取 `data.token_value`，后续请求添加：

```http
Content-Type: application/json
satoken: 登录返回的token_value
```

通过 `GET /auth/me` 可核对当前角色和权限。Token 有效期为 2 小时，通过鉴权的请求会续期；Token 生命周期与下文 30 分钟的咨询会话生命周期不同。

Swagger 操作：先执行登录，复制 `data.token_value`，点击 Authorize 输入原始 token，再执行 Agent 接口。

## 3. 请求参数

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `message` | string | 是 | 非空、不能全为空白、最长 4000 个 Java 字符单位；中文一般按一个单位计，部分 emoji 占两个 |
| `conversation_id` | string / null | 否 | 首次省略或传 null；续聊必须原样使用上一次成功响应返回的 ID |

会话 ID 必须匹配小写十六进制 UUID 外形：`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`。空字符串和大写不合法；接口只检查该格式，不限制 UUID 的版本位。

本版本兼容输入别名 `conversationId`，响应统一返回 `conversation_id`。建议客户端统一使用下划线命名，不要同时提交两个名称。旧版本可能忽略 `conversationId` 并新建会话，请更新后端后再测。

不要自行生成会话 ID。传入格式正确但不存在、过期或属于其他用户的 ID，会返回 404，不会自动创建指定 ID 的会话。

### 3.1 首次咨询

```json
{
  "message": "张老师，我高考580，福建位次23000，想学计算机"
}
```

这条请求没有提供科类。模型按指令应追问物理类或历史类，接口可以返回 HTTP 200、`code: 0`、`sources: []`。追问属于正常咨询结果，不代表失败或 Skill 未加载。

### 3.2 补充信息并续聊

```json
{
  "message": "我是物理类，优先考虑江浙沪，学费希望每年不超过一万元。",
  "conversation_id": "2ea2ab57-8624-4b54-aeb9-64bc37228262"
}
```

示例 UUID 仅展示格式，执行时替换为自己的上次响应 ID。续聊成功后返回相同的 `conversation_id`。如果再次省略该字段，就会创建新会话，不继承之前的分数或位次。

### 3.3 一次提供推荐信息

```json
{
  "message": "我是福建物理类考生，580分，位次23000，想学计算机，偏好江浙沪。请查询数据库并解释冲稳保推荐。"
}
```

`province`、`rank` 等不是本 HTTP 请求的独立字段。用户通过 `message` 提供信息，模型整理为内部工具参数。自然语言解析由模型完成，并非服务端确定性解析器。

## 4. 成功响应

HTTP 200；业务成功码为 `0`。下例为结构示例，回答文本与资料读取情况由本次模型结果决定，哈希仅作格式占位。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answer": "先确认科类：你是物理类还是历史类？补齐后再查询院校和冲稳保结果。",
    "sources": [],
    "conversation_id": "2ea2ab57-8624-4b54-aeb9-64bc37228262",
    "skill": {
      "name": "zhangxuefeng-skill",
      "entry": "skills/zhangxuefeng-skill/SKILL.md",
      "instructions_injected": true,
      "instructions_sha256": "0000000000000000000000000000000000000000000000000000000000000000",
      "resources_read": []
    }
  }
}
```

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `code` | integer | 成功为 0，失败通常与 HTTP 错误状态码相同 |
| `message` | string | 成功为 `success`，失败为错误说明 |
| `data.answer` | string | 非空的回答或追问，可能包含 Markdown；当前服务端接受最长 16000 个 Java 字符单位 |
| `data.sources` | array | 本轮实际完成的工具结果，按执行顺序排列；可为空，可能重复调用同一工具 |
| `data.conversation_id` | string | 当前用户的服务端会话 ID |
| `data.skill` | object | 本轮 Skill 指令注入和资料读取诊断信息 |

前端渲染 `answer` 时应使用安全的 Markdown 渲染设置，不执行返回内容中的 HTML/脚本。不要通过回答里的“我查过”代替对 `sources` 的核对。

### 4.1 Skill 状态

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `name` | string | 固定为 `zhangxuefeng-skill` |
| `entry` | string | 实际使用的项目适配入口路径；不是上游原文入口 |
| `instructions_injected` | boolean | 当前成功路径为 true，表示 Skill 指令已传入模型上下文；不保证模型遵循了所有要求 |
| `instructions_sha256` | string | 注入的入口正文与资料目录的 SHA-256，可和启动日志核对 |
| `resources_read` | string[] | 本轮实际读取的背景资料路径，按首次读取顺序去重，不含历史轮次 |

Skill 主体直接作为 system 消息传给模型，因此不会产生同名工具调用记录。只有模型需要阅读研究资料时，才会出现 `read_skill_resource`。

- `instructions_injected: true` 且 `sources: []`：入口已传入，本轮直接回答或追问。
- `resources_read` 非空：本轮额外读取了对应背景资料。
- 成功响应缺少 `skill`：检查是否运行旧构建、访问了其他实例，或中间层删改了字段。

启动日志包含 `Fixed skill loaded`、同一指令哈希和资料数量。加载的是项目适配版分析框架，上游原文仅为追溯保留。研究和示例中的统计、政策或人物陈述不是当前实时数据。

### 4.2 工具来源结构

每条来源的格式为 `{"tool": "工具名", "data": 工具结果}`。`data` 根据工具变化，客户端按 `tool` 分支解析，不要假设始终是数组。

| `tool` | `data` 结构 | 权限 | 用途 |
| --- | --- | --- | --- |
| `search_schools` | `SchoolSummary[]`，最多 10 条 | `school:read` | 院校列表查询 |
| `school_detail` | `SchoolDetail` | `school:read` | 院校及历史录取详情 |
| `recommend` | `RecommendationResult` | `recommend:use` | 冲稳保计算结果 |
| `read_skill_resource` | `SkillResource` | `recommend:use` | 背景研究或示例，不能当作实时业务证据 |

返回对象字段如下；数据库字段可能为 null，空数组表示没有匹配数据。

| 对象 | 字段 |
| --- | --- |
| `SchoolSummary` | `id`、`name`、`province`、`city`、`level`、`description` |
| `SchoolDetail` | 院校基本字段同上，另含 `admissions: Admission[]` |
| `Admission` | `province`、`subject_type`、`year`、`min_score`、`min_rank`、`major: Major` |
| `Major` | `id`、`name`、`category`、`description` |
| `RecommendationResult` | `message`、`recommendations`、`disclaimer` |
| `Recommendation` | `category`、`gap`、`score_gap`、`match_gap`、`school: SchoolSummary`、`major: RecommendedMajor`、`reason` |
| `RecommendedMajor` | `name`、`min_score`、`min_rank` |
| `SkillResource` | `path`、`description`、`kind`、`content`；`kind` 固定为 `background_reference` |

`recommendations` 是对象，键为 `冲`、`稳`、`保`，每组为 `Recommendation[]`，最多 5 条，也可为空。`gap` 与 `match_gap` 均为“历史最低位次 − 用户位次”；小于 -2000 为冲，大于 2000 为保，其余为稳。`score_gap` 为“用户分数 − 历史最低分”，不参与分类。前端应优先展示工具返回的 `category`、`reason` 和 `disclaimer`，不要根据模型文本重新计算分类。ID、位次、差值和分数为数值；描述与名称为字符串。

资料来源示例（`content` 此处缩写，实际为原文）：

```json
{
  "tool": "read_skill_resource",
  "data": {
    "path": "references/research/01-writings.md",
    "description": "著作与系统思考，分析框架的背景",
    "kind": "background_reference",
    "content": "这里是所读取的背景资料正文"
  }
}
```

## 5. 会话、并发与超时

| 行为 | 当前实现 |
| --- | --- |
| 会话存储 | Redis，按用户 ID 与会话 ID 隔离 |
| 会话过期 | 每次成功回复后续期 30 分钟；仅收到请求或请求失败不会续期 |
| 历史长度 | 最多 8 轮完整问答；序列化历史超过 32000 字符时继续删除最早的整轮；仅剩最新一轮时保留该轮 |
| 工具历史 | 工具明细不持久化；后续涉及数据的问题需重新查询 |
| 失败历史 | 模型、工具或会话处理失败不写入本轮问答；错误响应不含部分工具结果 |
| 并发 | 同一用户跨会话最多处理一条请求；其他请求返回 409 |
| 并发锁 | Redis 租约 10 分钟，正常结束释放；不等于正常请求预计耗时 |
| 历史提交 | 同一 Lua 操作校验锁所有者并写入历史；租约失效的旧请求返回 409，不能覆盖后续请求历史 |
| 频率限制 | 同一用户从首个计数请求起的 60 秒窗口最多 10 次；进入会话服务后即计数，后续 404/409/模型失败也占次数 |
| 模型与工具上限 | 最多 4 次模型请求、6 次工具调用；最后一轮必须产生最终回答，否则 502 |
| 模型超时 | 每次默认 30 秒，`LLM_TIMEOUT_SECONDS` 可设 1–60 秒；不是整条 Agent 请求的总超时 |
| 幂等 | 未实现幂等键和响应重放；不要在超时后立刻并行重复提交 |

客户端和网关的总超时应覆盖多轮模型调用及数据库、Redis 开销。当前没有整条请求的统一硬截止时间，也没有查询处理状态或取消任务接口。

## 6. 错误响应

业务异常格式：

```json
{
  "code": 404,
  "message": "会话不存在或已过期，请开始新会话",
  "data": null
}
```

| HTTP / code | 常见触发条件 | 客户端处理 |
| --- | --- | --- |
| 400 | message 缺失、空白、超长；会话 ID 格式不合法；JSON 无法解析 | 修正请求；校验错误文字可能包含 Java 字段名 `conversationId` |
| 401 | 没有 token、token 无效或过期 | 重新登录后再发请求 |
| 403 | 缺少 `recommend:use`，或实际调用院校工具时缺少 `school:read` | 核对账号权限；不要持续重试 |
| 404 | 会话不存在/过期/不属于当前用户；也可能是工具查询的学校不存在 | 根据 message 区分；会话失效时告知用户后开启新会话并补齐背景 |
| 409 | 同一用户已有咨询处理中；或请求处理结束时租约已失效 | 等待上一请求结束，避免同时发起多条咨询 |
| 429 | 超过用户请求频率限制 | 延后重试；当前不返回 `Retry-After` |
| 502 | 模型上游失败、无效响应、非法工具/参数、调用次数超限或读取未允许的 Skill 资料 | 可提示稍后重试；持续失败检查模型兼容性和日志 |
| 503 | LLM 未配置；认证或会话依赖不可用；模型请求中断；会话服务捕获的其他异常 | 检查后端配置与依赖，避免密集重试 |
| 504 | 某一次模型 HTTP 请求超时 | 提示超时；避免立即并行重发 |
| 500 | 其他未处理服务端错误 | 保存时间、HTTP 状态和 message，联系后端排查 |

HTTP 状态优先用于分支处理，`message` 用于提示，不作为稳定的程序枚举。上游原始错误正文和 API Key 不会返回客户端。代理自身产生的错误可能不是本 JSON 格式，客户端应兼容非 JSON 错误。

院校/推荐缓存读写失败会降级，不单独返回 503；认证和会话使用 Redis，依赖失败仍会阻止 Agent 请求完成。

## 7. 前端接入顺序

1. 登录并保存 token；新咨询界面将当前 `conversation_id` 置空。
2. 发送 `message`，存在会话 ID 时一并提交；等待期间禁用重复发送。
3. HTTP 200 且 `code == 0` 后展示 `answer`，保存响应 `conversation_id`。
4. 按 `tool` 展示本轮 `sources`；背景资料与院校、推荐数据分开标注。追问成功时不必要求 sources 非空。
5. Skill 诊断字段可放在开发调试信息中，无需对终端用户展示哈希和入口路径。
6. 开启新咨询时清空客户端 ID；现有后端没有主动删除会话的 HTTP 接口，旧会话按 TTL 过期。
7. 会话 404 时保留用户未发送成功的输入，提示重新开始，不静默丢失上下文。

接口不接受客户端指定的 system 提示词、模型名称或 Skill 名称。当前没有联网搜索；最新薪资、就业率和招生政策不在现有工具的数据能力内。

## 8. PowerShell 联调示例

先启动 MySQL、Redis 和已配置模型的后端。在 PowerShell 执行，演示账号可替换为实际账号：

```powershell
$base = "http://127.0.0.1:8080"
$loginBody = @{ username = "admin"; password = "Admin@123" } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($loginBody))
$headers = @{ satoken = $login.data.token_value }

# 首次咨询：故意缺少科类，检查追问及 Skill 状态
$firstBody = @{ message = "张老师，我高考580，福建位次23000，想学计算机" } | ConvertTo-Json
$first = Invoke-RestMethod -Method Post -Uri "$base/agent/chat" -Headers $headers `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($firstBody))
$first | ConvertTo-Json -Depth 20

# 续聊：使用本次成功响应的 ID，不能写死历史示例 UUID
$nextBody = @{
  message = "我是物理类，优先考虑江浙沪，请查询推荐数据。"
  conversation_id = $first.data.conversation_id
} | ConvertTo-Json
$next = Invoke-RestMethod -Method Post -Uri "$base/agent/chat" -Headers $headers `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($nextBody))
$next | ConvertTo-Json -Depth 20

$next.data.conversation_id -eq $first.data.conversation_id
$next.data.skill
```

最后的会话 ID 比较应为 `True`。模型可能因数据不足继续追问、查询无结果或返回有效业务错误，不能保证演示数据覆盖每个分数和偏好。

## 9. 检查范围与实现索引

本次检查修复了 camelCase 会话字段被忽略的问题，新增 `conversationId` 输入别名，输出和 Swagger 推荐字段仍为 `conversation_id`。

自动化验证覆盖：认证与权限、请求校验、两种会话字段、会话隔离/限流/锁、历史裁剪、工具参数与权限、Skill 指令传入 HTTP 请求、资料结果回传、诊断字段透传。模型请求使用本地模拟服务；这些检查不代表已完成真实模型质量、生产数据库、实际 Redis 或部署实例的端到端验收。

```powershell
cd backend
mvn -B package
```

主要实现：

- [AgentController](../backend/src/main/java/com/academic/planning/controller/AgentController.java)：HTTP 入口。
- [AgentChatRequest](../backend/src/main/java/com/academic/planning/dto/AgentChatRequest.java)、[AgentChatResponse](../backend/src/main/java/com/academic/planning/vo/AgentChatResponse.java)：请求响应结构。
- [AgentConversationService](../backend/src/main/java/com/academic/planning/service/AgentConversationService.java)：会话、频率限制、并发与历史。
- [AgentServiceImpl](../backend/src/main/java/com/academic/planning/service/impl/AgentServiceImpl.java)：Skill 注入、模型循环和工具执行。
- [SkillLoader](../backend/src/main/java/com/academic/planning/service/SkillLoader.java)：Skill 入口与资料白名单。
- [CompatibleLlmClient](../backend/src/main/java/com/academic/planning/service/impl/CompatibleLlmClient.java)：模型 HTTP 协议和超时。
- [AgentSkillIntegrationTest](../backend/src/test/java/com/academic/planning/service/AgentSkillIntegrationTest.java)：接口到模型 HTTP 的模拟链路验证。
