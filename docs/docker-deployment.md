# Docker 部署

在项目根目录运行以下命令。服务器只需安装 Docker Engine 和 Docker Compose v2；Java 17、Maven、MySQL 8.4、Redis 7.4 都由容器提供。前端仍通过微信开发者工具编译和发布。

Windows 上若 Docker Desktop 已运行但终端提示找不到 `docker`，先重开终端。按用户安装的 Docker Desktop 也可能位于 `%LOCALAPPDATA%\Programs\DockerDesktop`；可在 PowerShell 中仅为当前终端补上路径：

```powershell
$env:Path = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin;$env:Path"
docker version
docker compose version
```

如果不是该安装位置，以本机 Docker Desktop 的实际目录为准。

## 首次启动

### 2 GiB 小服务器

使用 `compose.server.yaml` 覆盖配置，在本机执行 `mvn package` 后将生成的 `backend/target/academic-planning-backend-1.0.0.jar` 放到部署目录的 `deploy/server/app.jar`。该方式只在服务器封装运行镜像，不在服务器编译或跑 Maven 测试；基础镜像会按服务器架构拉取，JAR 可复用。

后端、MySQL、Redis 的容器内存上限分别为 640、768、128 MiB；Java 堆上限 256 MiB，MySQL 最大连接数 30。它适合初步联调，实际承载能力需要根据负载观察。启动、查看状态和日志等命令都需同时指定两个 Compose 文件：

```bash
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker up -d --build --wait --wait-timeout 300
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker ps
```

### 通用配置

```bash
cp .env.docker.example .env.docker
```

Windows PowerShell 使用 `Copy-Item .env.docker.example .env.docker`。编辑 `.env.docker`，为 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`REDIS_PASSWORD` 分别设置不同的非空密码。包含 `$` 或 `#` 的值用单引号包裹，避免 Compose 插值。该文件已被 Git 忽略，不会进入镜像；不要覆盖原有的本地开发 `.env`。

如需 AI 聊天，将 `LLM_ENABLED` 改为 `true`，填写 `LLM_BASE_URL`、`LLM_API_KEY` 和 `LLM_MODEL`。默认关闭 LLM，注册、院校查询和规则推荐仍可使用。

```bash
docker compose --env-file .env.docker config --quiet
docker compose --env-file .env.docker up -d --build --wait --wait-timeout 300
docker compose --env-file .env.docker ps
curl --fail http://127.0.0.1:8080/health
```

首次构建会下载镜像与 Maven 依赖并运行后端测试，需要网络。后端使用普通数据库账号，等待 MySQL 和 Redis 健康检查通过后启动。`/health` 仅验证 HTTP 服务可达；注册、登录、院校查询还需实际验证数据库、Redis 和权限链路。

Windows 可执行以下冒烟测试（将端口改为实际的 `BACKEND_PORT`）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/smoke_docker.ps1 -BaseUrl http://127.0.0.1:18080
```

脚本会创建一个 `docker_test_` 前缀的测试账号，验证注册后的默认权限、院校查询、推荐和管理员接口拒绝访问，最后退出登录。测试账号会留在测试数据库中；不要对生产数据执行此测试。

初始化沿用项目演示数据，包括 `admin` / `Admin@123`。**在向外开放服务前，先登录并修改管理员密码。** 通过 SSH 部署时，可用 `ssh -L 8080:127.0.0.1:8080 用户@服务器` 将服务转发到开发电脑，再通过开发者工具完成登录和修改密码。

## 真机与服务器地址

默认只将后端映射到宿主机 `127.0.0.1:8080`，方便接宿主机上的 HTTPS 反向代理。MySQL 和 Redis 不映射宿主机端口。

临时直连测试时，把 `.env.docker` 的 `BACKEND_BIND_ADDRESS` 改成 `0.0.0.0`，重新运行启动命令，并在服务器安全组与防火墙中允许测试设备访问 `BACKEND_PORT`。小程序 `miniprogram/config/api.js` 的 `develop` 地址改成 `http://服务器IP:8080`；若部署在开发电脑，则填写电脑局域网 IP。手机上的 `127.0.0.1` 指向手机自身。

体验版和正式版使用 HTTPS 域名，并在微信公众平台配置 request 合法域名；同步填写小程序的 `trial` 和 `release` 地址。本配置提供后端 HTTP 服务，不自动申请证书。使用 Nginx 等代理时，AI 请求的读取超时建议设为 `300s`，与小程序咨询超时保持一致。

## 数据与更新

- `mysql-data` 保存账号、角色权限和院校数据；`redis-data` 保存 Redis AOF 数据。容器重建保留这些命名卷。
- MySQL **仅在空数据卷首次启动**时依次执行 `sql/schema.sql`、`sql/data.sql`。容器内禁用 Spring 的重复 SQL 初始化，避免每次重启覆盖演示数据或重新启用管理员。
- 修改 SQL 后，对已有数据库应备份并执行对应迁移；重新构建后端不会自动迁移旧数据，也不会把开发电脑数据库自动搬到服务器。
- 新建账号会按后端逻辑初始化普通用户角色及 `school:read`、`recommend:use` 权限。
- `.env.docker` 中的 MySQL 密码用于首次初始化；已有数据卷修改密码时，还需同步修改数据库用户密码，单改环境变量不会替换旧密码。
- `docker compose down` 保留数据卷；`down -v` 会删除数据，不要将它用于普通更新。

代码更新后：

```bash
docker compose --env-file .env.docker up -d --build --wait --wait-timeout 300
```

检查日志或停止服务：

```bash
docker compose --env-file .env.docker logs --tail=100 backend
docker compose --env-file .env.docker logs --tail=100 mysql redis
docker compose --env-file .env.docker down
```

若首次初始化 SQL 失败，先查看 MySQL 日志；不要在有业务数据的卷上用删除卷的方式解决问题。环境变量变更应运行 `up -d` 重建容器，单独 `restart` 不会更新容器环境。

容器启动依赖采用 [Compose 健康检查顺序](https://docs.docker.com/compose/how-tos/startup-order/)，MySQL 首次初始化行为参见[官方镜像入口脚本](https://github.com/docker-library/mysql/blob/master/8.4/docker-entrypoint.sh)。

## 修复初始化数据中文乱码

如果数据库中的“厦门大学”存成 `åŽ¦é—¨å¤§å­¦`，说明初始化 SQL 的 UTF-8 字节被按 Latin-1 解释。初始化文件现已显式设置 `SET NAMES utf8mb4`；已有数据库需要执行迁移，重建镜像不会自动修复。

在服务器项目目录运行：

```bash
bash scripts/repair-seed-encoding.sh
```

脚本短暂停止后端，先将数据库备份到 `backups/before-encoding-*.sql`，成功后才修复与已知种子数据精确匹配的乱码，同时修复招生科类约束。它保留账号密码、记录 ID 和录取分数，只清除院校及推荐缓存，然后启动后端并等待健康检查。备份含业务数据和密码哈希，应妥善保管，不要提交到仓库。

成功时输出 `Repair complete` 和备份路径；校验输出中的“厦门大学”十六进制应为 `E58EA6E997A8E5A4A7E5ADA6`。随后刷新小程序。迁移已在 MySQL 8.4 中验证可重复执行；它仅修复已知初始化文本，不会自动转换任意用户输入。若执行失败，保留备份和完整报错排查，不要删除数据库卷。