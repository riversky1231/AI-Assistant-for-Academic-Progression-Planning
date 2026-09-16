# 升学规划智能助手 B 组后端

本目录是原 FastAPI + SQLite 后端的 Spring 技术栈迁移版，只包含 B 组负责的后端能力：

- Spring Boot 3 + Java 17
- MyBatis-Plus + MySQL 8
- Redis 缓存
- Redis Token + 数据库 RBAC 登录与权限验证
- 学校列表、学校详情、冲稳保推荐
- Swagger/OpenAPI

不包含微信小程序、AI 问答和跨组联调代码。

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

应用启动时会幂等执行上级 `sql/schema.sql` 和 `sql/data.sql`，导入 18 所学校、54 个专业、54 条福建物理类 2025 年演示数据。

## 2. 启动与测试

```powershell
cd backend
mvn test
mvn spring-boot:run
```

Swagger：`http://127.0.0.1:8080/swagger-ui.html`

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
