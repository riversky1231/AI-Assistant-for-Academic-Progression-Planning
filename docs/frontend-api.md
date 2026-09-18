# 微信小程序接口契约（当前 Java 后端）

以 `backend/src/main/java/com/academic/planning/controller`、DTO、VO 和 `application.yml` 为准。服务默认端口为 8080，统一返回 `{code, message, data}`；Agent 使用 `/agent/chat`，推荐分类按位次差计算。

默认服务地址：`http://127.0.0.1:8080`。Swagger：`/swagger-ui.html`，OpenAPI：`/v3/api-docs`。小程序导入、环境域名与测试见 [使用说明](../miniprogram/README.md)。

## 公共约定

请求使用 `Content-Type: application/json`。响应使用统一包装：

```json
{ "code": 0, "message": "success", "data": {} }
```

`code=0` 表示成功；失败使用相应 HTTP 状态码及 `{ "code": 401, "message": "请先登录", "data": null }`。Java DTO/VO 使用 Jackson `SNAKE_CASE`，即 `subject_type`、`token_value`、`min_rank` 等。`/auth/me` 的 Map 键 `userId` 保持原样。

除登录、注册、微信登录、找回密码、健康检查外，业务接口必须携带登录响应指定的请求头，当前是：

```http
satoken: <token_value>
```

## 1. 用户与账号

### POST /auth/login

```json
{ "username": "账号", "password": "密码" }
```

账号不能为空，最多 50 字符；密码不能为空，长度 8–100。成功响应的 `data`：

```json
{
  "token_name": "satoken",
  "token_value": "服务端随机Token",
  "timeout": 7200,
  "permissions": ["school:read", "recommend:use"],
  "user": {
    "id": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "phone": null,
    "email": null,
    "wechat_bound": false,
    "enabled": true
  }
}
```

Token 在 Redis 中保存，每次通过鉴权后延长 2 小时有效期；不要把登录时的 `timeout` 当作不可续期的本地到期时间。错误账号或密码返回 401，同一用户名 5 分钟超过 10 次尝试返回 429。前端不自动重试登录。

### POST /auth/register

公开接口，成功后自动登录并返回同 `/auth/login` 的 Token 结构。账号 3–50 位，只允许字母、数字和下划线；密码 8–100 位；手机号、邮箱、昵称可选。

```json
{ "username": "student01", "password": "Student@123", "nickname": "小陈", "phone": "13800000000", "email": "student@example.com" }
```

普通注册账号默认分配 `user` 角色，拥有 `school:read` 和 `recommend:use`。

### POST /auth/wechat-login

公开接口，小程序调用 `wx.login` 后把 `code` 传给后端；当前项目使用演示型稳定 openid 生成逻辑，不调用真实微信服务端换取 openid。

```json
{ "code": "wx.login 返回的 code", "nickname": "微信用户" }
```

### POST /auth/forgot-password

公开接口，用已绑定手机号或邮箱校验后重置密码。

```json
{ "username": "student01", "phone": "13800000000", "email": null, "new_password": "NewPass@123" }
```

`phone` 与 `email` 至少匹配一项；同一账号 10 分钟最多尝试 5 次。

### GET /auth/profile

已登录。返回当前账号的安全资料，不包含密码哈希。

### PUT /auth/profile

已登录。更新昵称、手机号、邮箱。

```json
{ "nickname": "小陈", "phone": "13800000000", "email": "student@example.com" }
```

### POST /auth/change-password

已登录。校验原密码后修改。

```json
{ "old_password": "OldPass@123", "new_password": "NewPass@123" }
```

### GET /auth/me

`data` 示例：

```json
{ "userId": 1, "permissions": ["school:read", "recommend:use"], "roles": ["admin"] }
```

### POST /auth/logout

无需请求体。删除当前 Redis Token，成功 `data=null`。小程序成功后清理 Token、本机考生档案和最近推荐。

### 账号管理接口

以下接口需要登录且拥有 `account:manage` 权限，默认管理员 `admin` 拥有该权限。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/auth/users` | 查询账号列表，响应为 `UserVO[]` |
| POST | `/auth/users` | 管理员创建账号 |
| PUT | `/auth/users/{userId}` | 修改昵称、手机号、邮箱、启停状态 |
| POST | `/auth/users/{userId}/reset-password` | 管理员重置指定账号密码 |

## 2. 健康检查

`GET /health` 无需登录。成功响应：

```json
{ "code": 0, "message": "success", "data": { "status": "ok" } }
```

此接口说明 HTTP 服务可达，不是 MySQL、Redis、账号权限的全面检查。

## 3. 院校列表

`GET /schools`，需要 `school:read` 权限。

| 参数 | 约束 | 含义 |
|---|---|---|
| keyword | 可选，1–50 字符 | 校名或简介关键词；空值应省略 |
| province | 可选，2–20 字符 | 学校所在地，不是生源地 |
| limit | 默认 20，范围 1–50 | 条数上限，无分页游标和总数 |

成功 `data` 是数组，字段为 `id`、`name`、`province`、`city`、`level`、`description`。无匹配时为 `[]`。前端只能称“本次展示 N 所”，不能将其作为数据库院校总数。

## 4. 院校详情

`GET /schools/{schoolId}`，正整数 ID，需要 `school:read` 权限。不存在返回 404。

成功 `data` 的结构示例（非实时数据）：

```json
{
  "id": 2,
  "name": "福州大学",
  "province": "福建",
  "city": "福州",
  "level": "211",
  "description": "国家双一流建设高校。",
  "admissions": [
    {
      "province": "福建",
      "subject_type": "物理类",
      "year": 2025,
      "min_score": 609,
      "min_rank": 12500,
      "major": { "id": 4, "name": "计算机科学与技术", "category": "工学", "description": "学习计算机系统、软件与算法基础。" }
    }
  ]
}
```

`admissions[].province` 为考生生源地。当前 Java VO **没有**旧版的 `source_name`、`source_url`、`subject_group` 字段，不应虚构来源链接。现有 SQL 为演示数据，页面明确标注历史数据仅供参考。

## 5. 冲稳保推荐

`POST /recommend`，需要 `recommend:use` 权限。

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

省份必填，2–20 字符；科类仅允许 `物理类` / `历史类`；分数为 0–750 的整数，位次为 1–10000000 的整数。两个偏好可省略或传 `null`，均最多 50 字符。地区可用 `江浙沪`、`长三角` 或顿号、英文/中文逗号、斜杠分隔的省市。

成功 `data`：

```json
{
  "message": "已按历史最低位次生成冲、稳、保建议。",
  "recommendations": {
    "冲": [],
    "稳": [
      {
        "category": "稳",
        "gap": 1000,
        "score_gap": -10,
        "match_gap": 1000,
        "school": { "id": 2, "name": "示例院校", "province": "福建", "city": "福州", "level": "211", "description": "示例" },
        "major": { "name": "软件工程", "min_score": 590, "min_rank": 16000 },
        "reason": "历史最低分 590、最低位次 16000；您的位次差 +1000 名，按位次判定为“稳”。"
      }
    ],
    "保": []
  },
  "disclaimer": "推荐基于本地演示数据与规则，仅供参考，不承诺录取结果。"
}
```

以上数值仅用于说明格式。`gap = 历史最低位次 - 用户位次`，`gap < -2000` 为冲，`-2000..2000` 为稳，`gap > 2000` 为保。`match_gap` 当前等于 `gap`，分数不参与分类。后端每类按 `abs(gap)` 升序最多取 5 条，前端保留原顺序。记录单位为院校专业组合，不是不同学校数，不应转换为录取概率。

三个数组全空是正常结果，不是系统错误。当前种子数据覆盖福建物理类，其他生源地/科类可能无数据。推荐响应不含年份，具体历史年份到院校详情中查看，不在结果页推测。

## 异常处理

| HTTP 状态 | 小程序行为 |
|---|---|
| 400 | 显示参数校验信息，并保留表单方便修改 |
| 401 | 业务接口清理本机账号数据、跳转登录；登录接口显示账号或密码错误 |
| 403 | 显示权限不足，不重复跳转登录 |
| 404 | 显示院校或服务不存在 |
| 409 | 显示账号、手机号或邮箱已存在 |
| 429 | 提示稍后重试，不自动重发 |
| 500 / 503 | 显示服务异常，提供重试入口 |
| 网络错误 / 超时 | 显示连接或超时提示，结束加载状态 |

当前前端已使用登录、注册、找回密码、微信登录、资料维护、改密码和管理员账号管理接口；没有调用收藏、档案云同步等接口。
