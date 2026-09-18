# 升学规划智能助手

当前主后端为 Spring Boot 工程，前端为原生微信小程序。Java 后端提供：

- Spring Boot 3 + Java 17
- MyBatis-Plus + MySQL 8
- Redis 缓存
- Redis Token + 数据库 RBAC 登录与权限验证
- 学校列表、学校详情、冲稳保推荐
- Swagger/OpenAPI

包含基于工具调用的 Agent/LLM 问答接口和固定张雪峰视角。原生微信小程序包含首页、院校库、院校详情、志愿推荐、冲稳保结果、我的和账号登录；使用微信开发者工具导入仓库根目录，配置见 [小程序使用说明](miniprogram/README.md)，接口见 [前端接口文档](docs/frontend-api.md)。

后端统一使用 Java，Agent 对话入口为 `/agent/chat`。运行与测试使用 JDK、Maven、Node.js 和 PowerShell。

## 1. 初始化环境

先创建数据库：

```sql
CREATE DATABASE academic_planning
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

启动 MySQL 和 Redis，然后配置环境变量：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的MySQL密码"
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
```

从 `backend` 启动时会执行上级 `sql/schema.sql` 和 `sql/data.sql`，导入 18 所学校、54 个专业，以及福建物理类和其他省份物理类/历史类的 2025 年演示数据。已有数据库请先核对初始化脚本是否适用于当前数据；推荐数据均须与当年官方信息核验。

## 2. 启动与测试

```powershell
cd backend
mvn test
mvn spring-boot:run
```

Swagger：`http://127.0.0.1:8080/swagger-ui.html`

小程序服务地址在 `miniprogram/config/api.js` 中设置，请按电脑或真机可访问的地址配置。前端检查在仓库根目录执行：

```powershell
node --test tests/miniprogram.test.cjs
powershell -NoProfile -ExecutionPolicy Bypass -File tests/check_miniprogram_markup.ps1
```

演示账号：

```text
用户名：admin
密码：Admin@123
```

登录后，将返回的 token 放入请求头：

```text
satoken: <tokenValue>
```

## 3. 接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/health` | 公开 | 健康检查 |
| POST | `/auth/login` | 公开 | 登录 |
| POST | `/auth/logout` | 已登录 | 退出 |
| GET | `/auth/me` | Redis Token | 当前角色和权限 |
| GET | `/schools` | `school:read` | 学校列表和筛选 |
| GET | `/schools/{id}` | `school:read` | 学校、专业和录取详情 |
| POST | `/recommend` | `recommend:use` | 按位次差生成冲稳保 |
| POST | `/agent/chat` | `recommend:use` | Agent 问答，学校工具另需 `school:read` |

推荐请求：

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

推荐分类只采用 `历史最低位次 - 用户位次`：小于 `-2000` 为“冲”，大于 `2000` 为“保”，其余为“稳”。分数只作为展示信息，不参与分类。

## 4. Agent 与 LLM 接入

前端联调、请求响应字段、会话续聊、Skill 状态和错误处理详见 [Agent 接口文档](docs/Agent接口文档.md)。

接入支持 Chat Completions function calling 的模型服务。默认关闭，不影响现有接口。支持从项目根目录或 `backend` 启动时自动读取根目录 `.env`，也可设置进程环境变量（环境变量优先）。`.env` 使用 `KEY=value` 格式，不加引号或 `export` 前缀；请勿提交密钥。环境变量配置示例：

```powershell
$env:LLM_ENABLED = "true"
$env:LLM_BASE_URL = "https://你的服务地址/v1"
$env:LLM_MODEL = "服务商提供的模型名称"
$env:LLM_API_KEY = "你的密钥"
$env:LLM_TIMEOUT_SECONDS = "30"
cd backend
mvn spring-boot:run
```

`LLM_BASE_URL` 为服务商的 API 基础地址，程序追加 `/chat/completions`；是否包含 `/v1` 以服务商文档为准。模型需支持 `tools`、`tool_choice=auto` 和非流式响应。

密钥通过 `System.getenv("LLM_API_KEY")` 显式优先读取当前进程环境变量；未设置时才回退到应用配置。显式设置为空会禁用密钥回退。启动日志显示 `LLM API key: loaded=true, source=process environment LLM_API_KEY`，不输出密钥。修改环境变量后需重新启动后端；从 IDE 启动时，确保运行配置或 IDE 进程已继承该变量。

登录后携带 `satoken` 请求头，向 `POST /agent/chat` 提交：

```json
{"message":"我是福建物理类考生，580分、15000位，想学计算机，偏好江浙沪，请推荐学校并解释冲稳保。"}
```

响应沿用 `ApiResponse`，`data.answer` 为模型回答，`data.sources` 为本次实际执行的工具及结果。工具包括 `search_schools`、`school_detail`、`recommend` 和 `read_skill_resource`。推荐沿用既有规则，模型负责解释；前端可用 sources 展示可核对的数据。`read_skill_resource` 的结果包含 `path`、`description`、`kind: background_reference` 和 `content`，应显示为背景资料，不能当作实时招生或就业数据。

首次请求省略 `conversation_id`，响应的 `data.conversation_id` 用于后续咨询：

```json
{"message":"如果改成电子信息专业呢？","conversation_id":"上一次响应的会话 UUID"}
```

会话保存在 Redis，按用户隔离，成功回复后续期 30 分钟。保留最近最多 8 轮完整问答，并按序列化字符预算裁剪；工具明细不持久化，涉及数据的后续问题需重新查询。会话过期或属于其他用户时返回 404，省略 ID 可开始新会话。模型失败不会写入本轮历史。Redis 不可用时返回 503。

每用户每分钟最多 10 次请求，同一用户同时只处理一条咨询，超过分别返回 429、409。并发租约使用 Redis 原子操作和所有者校验，支持多个应用实例；异常退出后租约最多 10 分钟自动释放。

当前不支持流式输出。单请求最多 4 轮模型请求、6 次工具调用，每轮默认 30 秒超时（配置范围 1–60 秒）。工具参数和权限由服务端校验，模型不能调用任意方法或 SQL。

未配置模型返回 503，上游失败/无效工具调用返回 502，模型超时返回 504。不会向客户端返回上游错误正文或密钥。当前数据库是演示历史数据，回答不代表实际录取承诺。用户问题和工具查询结果会发送到配置的模型服务。

自动化测试通过本地模拟 HTTP 服务验证协议与错误处理，不需要真实 API Key；真实服务商联调需配置后执行上述请求。

## 5. 默认绑定张雪峰视角

Agent 每轮自动使用张雪峰视角的分析框架，无需说“切换到张雪峰”。框架包含就业倒推、家庭承受能力、学校与城市、长期技能和试错成本，指导模型先了解条件、查询数据、比较取舍，再给行动建议。它不冒充真人，也不代表本人意见。

Skill 位于 `backend/skills/zhangxuefeng-skill/`：

- `SKILL.md`：本项目适配入口，每轮作为固定咨询指令加载。
- `resource-index.json`：允许读取的六份研究资料和一份对话示例目录。
- `references/research/`、`examples/`：模型通过 `read_skill_resource` 按需读取，计入原有工具次数限制。
- `UPSTREAM-SKILL.md`：保留上游原文用于追溯，不作为运行指令加载。
- `SOURCE.md`、`LICENSE`：来源提交、适配说明和上游 MIT 许可证。

`SkillLoader` 在启动时校验并缓存入口和目录内文件；缺失必需文件会启动失败。Maven 将 Skill 打包进 JAR，运行时无需联网下载、不依赖当前工作目录。修改 Skill 后需重新构建并重启。IDE 直接启动时，应先运行 `mvn process-resources` 或使用 Maven 构建以复制资源。

资料工具只接受目录内的精确路径，不开放任意文件、网络或脚本执行。读取资料要求 `recommend:use`，查询学校仍要求 `school:read`。会话历史沿用 Redis，框架每轮重新注入，无需额外保存激活状态。

当前没有联网搜索工具，不能查询最新就业率、薪资或政策。上游研究及示例只作背景资料，其中数字不当作当前事实。院校与冲稳保仍使用数据库工具；模型负责解释，不修改计算规则。固定框架是模型指令，尚未增加用于强制每个咨询步骤的服务端状态机。

可用普通问题验证默认模式：

```json
{"message":"我想选计算机专业，但担心家庭负担和毕业就业，该怎么比较？"}
```

回答应围绕成本、目标和选择条件展开，缺少最新就业证据时说明局限。资料阅读可通过 `data.sources` 中的 `read_skill_resource` 核对；是否需要读资料由模型决定，并非每次必调。

### 检查 Skill 是否加载

`sources` 为空不代表 Skill 没有加载：入口指令每轮直接加入 system 消息，只有模型主动读取参考资料时才产生 `read_skill_resource` 工具记录。成功响应新增 `data.skill`：

```json
{
  "name": "zhangxuefeng-perspective",
  "entry": "skills/zhangxuefeng-skill/SKILL.md",
  "instructions_injected": true,
  "instructions_sha256": "本次注入的入口正文及资料目录的 SHA-256",
  "resources_read": []
}
```

`instructions_injected` 表示已传入模型上下文，不保证模型遵循了每条要求；`resources_read` 只列本次实际读取的参考资料。启动日志 `Fixed skill loaded` 提供同一指令哈希和资料数量，用于核对部署版本，不打印密钥或用户消息。若成功响应没有 `data.skill`，检查是否仍在运行旧进程或请求了其他实例；修改源码后需重新构建、重启。

自动化接口链路测试覆盖 `/agent/chat` 的鉴权、会话包装、真实 HTTP 请求序列化及响应字段，使用本地模拟模型验证指令确实发送、资料内容确实回传。它不替代真实模型的回答质量测试。
