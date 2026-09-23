# OpenResty Plus

企业级多中心 OpenResty/Nginx 配置管理平台 MVP。

## 目录

- `backend/`：Spring Boot 4.1.1 控制面，Java 21，Maven
- `frontend/`：官方 Vben Admin v5.7.0，`@vben/web-antd`
- `deploy/`：Control API 节点镜像和受限发布脚本
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

停止并保留数据卷：

```bash
docker compose down
```

清理开发数据库和 Redis 数据卷：

```bash
docker compose down -v
```

Compose 的两个测试节点使用 `deploy/openresty/Dockerfile.control-api`：Nginx 1.31.5，启用官方 Control API、HTTP Lua 和 Stream Lua。Control API 实际监听容器内 Unix Socket `/run/openresty/control.sock`；节点启动脚本通过只在 Docker 私有网络可见的 `socat` 端口转发给控制面，宿主机不会暴露该管理端口。

Compose 不创建 MySQL 或 Redis 容器，控制面直接连接 `.env` 中配置的外部服务。

运行时配置以 MySQL 的不可变版本快照为准。Lua 规则可以按版本热更新；新增监听端口、修改原生 HTTP/Stream 块等必须调用本机 Control API 执行 reload。

运行配置 API：

- `POST /api/centers/{centerId}/runtime-configurations` 发布当前配置。
- `GET /api/centers/{centerId}/runtime-configurations/current` 读取节点轮询的最新快照。
- `POST /api/centers/{centerId}/runtime-configurations/{versionNo}/rollback` 从历史快照创建新的回滚版本。

原生配置渲染 API：

- `GET /api/centers/{centerId}/native-configurations/preview` 预览由数据库配置生成的完整 Nginx include 树和校验和。
- `POST /api/centers/{centerId}/native-configurations/materialize` 原子写入控制面渲染目录，供节点下发、`nginx -t` 和 Control API reload 编排读取。
- `POST /api/centers/{centerId}/reload` 对中心内已启用节点执行 Control API reload，并记录逐节点结果。

表单字典 API：

- `GET /api/dictionaries/HTTP_CONTENT_TYPE`：返回已启用的 Content-Type 下拉项。
- `GET /api/dictionaries/HTTP_METHOD`：返回已启用的 HTTP 方法下拉项。
- `GET /api/dictionaries/{type}/all` 及同路径的 `POST`、`PUT`、`DELETE`：字典管理接口，可维护自定义值、排序和启停状态。

这些接口在生产 profile 下由全局 OAuth2 JWT 资源服务器保护；`local` profile 为本地 Compose 联调保留免登录能力，不能用于生产环境。

### Host 网络模式与动态端口

容器以默认 bridge 网络启动时，宿主机仅能访问 Compose `ports` 中显式发布的端口。要让动态生成的 `listen` 端口直接绑定 Docker 宿主机，使用 Host 网络覆盖文件启动**单个**节点：

```bash
docker compose -f docker-compose.yaml -f docker-compose.host-network.yaml \
  up -d --build control-plane openresty-east-1
```

该模式下，HTTP/Stream 的新增监听端口在“生成原生配置并重载”成功后会直接由宿主机监听。控制面通过 `http://host.docker.internal:8080` 被该节点访问；请在“中心与节点”把该节点的 Control API 配成 `http://host.docker.internal:18081`。

同一宿主机只能运行一个使用相同监听端口的 Host 网络 OpenResty 容器。多节点应部署到不同主机；若必须同机运行，所有 HTTP、Stream 和 Control API 管理端口都必须互不冲突。

HTTP Upstream 支持在界面中维护多个后端实例：地址、端口、权重、最大失败次数、失败判定时间、备用实例和启停状态。原生渲染会只写入已启用实例，并生成对应的 `server`、`weight`、`max_fails` 与 `fail_timeout` 指令；未配置启用实例时会保留一个 `down` 占位，确保 `nginx -t` 可通过且不会误转发流量。启用 TLS 的 HTTP server 也会在预览中给出证书引用缺失提示，生成监听保持为 HTTP，避免产生无法通过校验的配置。
