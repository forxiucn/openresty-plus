# OpenResty Plus

企业级多中心 OpenResty/Nginx 配置管理平台 MVP。

## 目录

- `backend/`：Spring Boot 4.1.1 控制面，Java 21，Maven
- `frontend/`：官方 Vben Admin v5.7.0，`@vben/web-antd`
- `deploy/`：OpenResty 节点镜像、运行时 Lua 和节点注册脚本
- `docs/`：ADR 与开发决策
- `CONTEXT.md`：领域词汇

## 本地开发

1. 复制 `.env.example` 到未提交的 `.env`，填入本地 MySQL/Redis 凭据。
2. 使用 `SPRING_PROFILES_ACTIVE=local` 启动后端；local profile 仅用于本地开发，不用于生产。
3. 在 `backend/` 执行 `mvn spring-boot:run`。
4. 在 `frontend/` 使用 Node 22.18.0 和 pnpm 10.33.4 执行 `pnpm install`，再执行 `pnpm -F @vben/web-antd run dev`。

## Docker 联调

在项目根目录准备 `.env`（设置外部 MySQL/Redis 的连接信息），然后执行：

```bash
docker compose up -d --build
```

服务入口：

- 管理 Web：<http://localhost:5173>
- Spring Boot API：<http://localhost:8080>
- OpenResty 节点 1：<http://localhost:18080/health>
- OpenResty 节点 2：<http://localhost:28080/health>
- Swagger UI：<http://localhost:8080/swagger-ui.html>

停止容器（配置目录保留在项目中）：

```bash
docker compose down
```

Compose 不创建 MySQL、Redis 或 Docker named volume。配置渲染目录直接绑定到 `./runtime/native-config/`。

Compose 的两个测试节点使用 `deploy/openresty/Dockerfile.control-api`：Nginx 1.31.5，启用官方 Control API、HTTP Lua 和 Stream Lua。当前 `docker-compose.yaml` 中所有服务使用 host 网络；前端监听 5173，两个测试节点分别使用 18080/28080，Control API 转发端口分别为 18081/28081。

`node-registration` 服务会在控制面可用后，将测试节点 `openresty-east-1` 自动登记到示例中心 `c62981ca-9bb7-4ab4-b871-5c9943efe84d`，Control API 地址为 `http://127.0.0.1:18081`。已删除的节点不会在重启时被重新创建。

Compose 不创建 MySQL 或 Redis 容器，控制面直接连接 `.env` 中配置的外部服务。

### 实例日志实时链路

每个 OpenResty 节点配套一个 Filebeat 容器，读取绑定到项目目录的 `/var/log/nginx`，向 `192.168.100.6:9092` 的 `dtl-602-openresty-plus` topic 发送 JSON 日志。控制面使用 Spring Kafka 消费该 topic，并通过 `/api/centers/{centerId}/nodes/{nodeId}/logs/stream` 以 SSE 推送给 Web 控制台；页面进入“实例日志”后按中心、实例和访问/错误日志类型实时显示。Filebeat 的节点标识默认使用 Compose 服务名 `openresty-east-1` / `openresty-east-2`，生产部署时可通过环境变量覆盖。

运行时配置以 MySQL 的不可变版本快照为准。Web 表单保存只写入 MySQL；点击发布后，OpenResty Lua 工作进程按默认 5 秒轮询读取新快照。新增监听端口、修改原生 HTTP/Stream 块等还必须生成原生配置并调用节点 Control API 执行 reload。

运行配置 API：

- `POST /api/centers/{centerId}/runtime-configurations` 发布当前配置。
- `GET /api/centers/{centerId}/runtime-configurations/current` 读取节点轮询的最新快照。
- `POST /api/centers/{centerId}/runtime-configurations/{versionNo}/rollback` 从历史快照创建新的回滚版本。

原生配置渲染 API：

- `GET /api/centers/{centerId}/native-configurations/preview` 预览由数据库配置生成的完整 Nginx include 树和校验和。
- `POST /api/centers/{centerId}/native-configurations/materialize` 原子写入控制面渲染目录，供节点下发、`nginx -t` 和 Control API reload 编排读取。
- `POST /api/centers/{centerId}/reload` 对中心内已启用节点执行 Control API reload，并记录逐节点结果。
- `POST /api/centers/{centerId}/deployments` 一次完成发布快照、生成原生配置和所有启用节点 reload。

表单字典 API：

- `GET /api/dictionaries/HTTP_CONTENT_TYPE`：返回已启用的 Content-Type 下拉项。
- `GET /api/dictionaries/HTTP_METHOD`：返回已启用的 HTTP 方法下拉项。
- `GET /api/dictionaries/{type}/all` 及同路径的 `POST`、`PUT`、`DELETE`：字典管理接口，可维护自定义值、排序和启停状态。

这些接口在生产 profile 下由全局 OAuth2 JWT 资源服务器保护；`local` profile 为本地 Compose 联调保留免登录能力，不能用于生产环境。

### Host 网络模式与动态端口

项目已统一使用 host 网络，不再使用 `docker-compose.host-network.yaml`。HTTP/Stream 新增监听端口在“生成原生配置并重载”成功后直接绑定宿主机。由于 host 网络共享宿主机端口，同一主机上的节点、前端和控制面监听端口必须互不冲突；生产环境应按节点主机规划端口。

HTTP Upstream 支持在界面中维护多个后端实例：地址、端口、权重、最大失败次数、失败判定时间、备用实例和启停状态。原生渲染会只写入已启用实例，并生成对应的 `server`、`weight`、`max_fails` 与 `fail_timeout` 指令；未配置启用实例时会保留一个 `down` 占位，确保 `nginx -t` 可通过且不会误转发流量。启用 TLS 的 HTTP server 也会在预览中给出证书引用缺失提示，生成监听保持为 HTTP，避免产生无法通过校验的配置。
