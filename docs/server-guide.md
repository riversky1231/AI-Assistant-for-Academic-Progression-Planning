# 雪乐兹服务器操作教程

当前部署地址为 `47.114.33.144`，Linux 服务器项目目录为 `/opt/xuelezi`，配置按 2 核、2 GiB 内存准备。后端、MySQL、Redis 由 Docker 运行；小程序在电脑上的微信开发者工具中编译。完整配置说明见 [Docker 部署指南](docker-deployment.md)。

## 1. 连接服务器

在 **Windows 电脑的 PowerShell** 执行：

```powershell
ssh -i "C:\Users\ASUS\Desktop\key\_key1.pem" root@47.114.33.144
```

连接断开后重新执行即可，容器不会随 SSH 断开而停止。首次连接先通过云控制台核对主机指纹，再确认连接。`Permission denied (publickey)` 表示需要检查用户名、私钥路径和云控制台绑定的密钥；`UNPROTECTED PRIVATE KEY FILE` 表示应在 Windows 文件属性的安全设置中关闭继承并移除其他普通用户或组的读取权限，保留自己的访问权限。不要上传私钥到服务器或仓库。

在 **服务器终端** 确认 Docker Engine 和 Compose v2 可用：

```bash
docker version
docker compose version
```

新服务器尚未安装时，按 [Docker 官方 Ubuntu 安装说明](https://docs.docker.com/engine/install/ubuntu/)安装 Engine 和 Compose 插件。当前服务器已有 Docker，不必重复安装。镜像拉取超时，先检查已配置的可信镜像加速器和网络，见本文故障排查。

## 2. 本机构建并上传

在 **Windows PowerShell 的项目根目录** 执行，需要本机 Java 17 和 Maven：

```powershell
mvn -f backend/pom.xml clean package
if ($LASTEXITCODE -ne 0) { throw '构建失败，停止上传' }
Copy-Item backend/target/academic-planning-backend-1.0.0.jar deploy/server/app.jar
tar -czf xuelezi-server.tar.gz compose.yaml compose.server.yaml .env.docker.example deploy/server/Dockerfile deploy/server/.dockerignore deploy/server/app.jar sql scripts
scp -i "C:\Users\ASUS\Desktop\key\_key1.pem" xuelezi-server.tar.gz root@47.114.33.144:/root/
```

若电脑只装了 Docker，可以先执行 `docker build --target build -t xuelezi-build:local .`，然后用 `docker create --name xuelezi-build-export xuelezi-build:local` 创建导出容器，用 `docker cp xuelezi-build-export:/workspace/backend/target/academic-planning-backend-1.0.0.jar deploy/server/app.jar` 提取 JAR，最后执行 `docker rm xuelezi-build-export`；每步成功后再继续打包上传。构建阶段会执行后端测试。

上传包不包含本地 `.env.docker`，服务器密码和模型密钥单独配置。PowerShell 的 `$env:TEMP` 不能直接复制到 CMD 使用；以上使用当前目录的文件名，两种终端的 scp 均可识别。

## 3. 首次部署

在 **服务器终端** 执行：

```bash
mkdir -p /opt/xuelezi
tar -xzf /root/xuelezi-server.tar.gz -C /opt/xuelezi
cd /opt/xuelezi
test -f .env.docker || cp .env.docker.example .env.docker
chmod 600 .env.docker
nano .env.docker
```

填写三个不同的数据库及 Redis 密码。公网直连调试设置：

```dotenv
BACKEND_BIND_ADDRESS=0.0.0.0
BACKEND_PORT=8080
```

启动并检查：

```bash
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker config --quiet
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker up -d --build --wait --wait-timeout 300
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker ps
curl --fail http://127.0.0.1:8080/health
```

健康检查预期返回 `{"code":0,"message":"success","data":{"status":"ok"}}`。首次空数据库自动导入演示数据；已有数据库不会再次初始化。对外开放前修改演示管理员密码，操作方式见 Docker 部署指南。

云控制台安全组允许测试设备来源访问 TCP 8080，SSH 使用 TCP 22；同时检查服务器防火墙。无需开放 MySQL 3306 或 Redis 6379。在 **Windows 电脑** 验证公网链路：

```powershell
curl.exe --noproxy "*" -i --connect-timeout 10 http://47.114.33.144:8080/health
```

小程序 `miniprogram/config/api.js` 的 `develop` 已指向这个公网地址，重新编译后测试注册、中文院校查询和推荐。手机上的 `127.0.0.1` 指手机自身。体验版、正式版需配置 HTTPS 域名及微信 request 合法域名，并填入 `trial`、`release`。

## 4. 启用模型

在 `/opt/xuelezi` 编辑 `.env.docker`，按模型服务商提供的参数填写：

```dotenv
LLM_ENABLED=true
LLM_BASE_URL=填写模型接口基础地址
LLM_API_KEY=填写真实密钥
LLM_MODEL=填写模型标识
LLM_TIMEOUT_SECONDS=30
```

重新创建后端以加载环境变量：

```bash
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker up -d --no-deps --force-recreate --wait --wait-timeout 300 backend
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker logs --tail=100 backend
```

在小程序发起一次 AI 对话验证；`/health` 成功不代表模型密钥和额度可用。不要将真实密钥写进教程或提交 Git。

## 5. 后续更新与备份

先备份数据库。在 **服务器项目目录** 执行：

```bash
umask 077
mkdir -p backups
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker exec -T mysql sh -c 'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump -uroot --single-transaction --no-tablespaces --default-character-set=utf8mb4 "$MYSQL_DATABASE"' > "backups/before-update-$(date +%Y%m%d-%H%M%S).sql"
```

确认命令成功且备份非空，再按第 2 节重新构建、上传。在服务器更新：

```bash
cd /opt/xuelezi
tar -xzf /root/xuelezi-server.tar.gz -C /opt/xuelezi
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker up -d --build --wait --wait-timeout 300
curl --fail http://127.0.0.1:8080/health
```

保留服务器原有 `.env.docker` 和命名数据卷。SQL 变更应按对应迁移说明单独执行；不要重新导入整个种子文件，也不要使用 `down -v`。普通 `restart` 不会加载新的环境变量。备份应另存到受控位置，恢复前先停止写入并核对目标数据库。

## 6. 中文乱码修复

本版初始化 SQL 已显式指定 UTF-8。旧库若出现 `åŽ¦é—¨å¤§å­¦`，更新文件后在服务器项目目录执行：

```bash
bash scripts/repair-seed-encoding.sh
```

脚本先停止后端并备份，随后修复已知种子文本、招生科类约束，清除院校及推荐缓存，最后启动后端。保留账号密码、记录 ID 和录取分数。看到 `Repair complete` 后刷新小程序；输出的“厦门大学”十六进制应为 `E58EA6E997A8E5A4A7E5ADA6`。若失败，保留备份及完整错误，按错误排查，勿删除数据卷。它不自动修复任意用户输入的乱码。

## 7. 常见故障

| 现象 | 检查方式 |
|---|---|
| Docker Hub 拉取超时 | 检查 `/etc/docker/daemon.json` 中已有的可信 `registry-mirrors` 配置，保留其他配置项；用 `docker info` 查看是否生效，使用 `docker pull mysql:8.4` 验证。修改后重启 Docker 会影响运行中的服务，应安排维护时间。 |
| 服务器本机健康检查成功，电脑超时 | 检查 `docker compose ... ps` 是否显示 `0.0.0.0:8080->8080/tcp`，以及云安全组、系统防火墙和来源 IP。 |
| 浏览器 502 | 用上面的 `curl.exe --noproxy "*"` 排除电脑代理；如果仍失败，检查反向代理配置及后端日志。 |
| 真机仍请求 127.0.0.1 | 检查小程序环境地址并重新编译、预览，确认当前使用的版本。 |
| 后端 unhealthy | 执行下面的日志命令，检查数据库连接、Redis 密码及内存不足等具体错误。 |
| 修改数据库环境密码后登录失败 | 已有数据卷不会自动修改 MySQL 用户密码，需同步调整数据库用户凭据。 |

```bash
cd /opt/xuelezi
docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker logs --tail=100 backend mysql redis
docker stats --no-stream
```
