# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

企业级多中心 OpenResty/Nginx 配置管理平台。Spring Boot 控制面管理配置、策略和发布；前端 Vben Admin 提供可视化操作；节点通过 Control API 拉取配置并 reload。

## 开发环境要求

- **Node**: 22.18.0 LTS（前端必须）
- **pnpm**: 10.33.4（前端必须）
- **Java**: 21 LTS
- **Maven**: 3.9+
- **MySQL**: 外部依赖，通过环境变量连接
- **Redis**: 外部依赖，通过环境变量连接

## 常用命令

### 后端

```bash
cd backend
mvn spring-boot:run                          # 本地启动（默认 local profile）
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run  # 显式指定 local profile
mvn test                                     # 运行所有测试
mvn test -Dtest=HttpUpstreamControllerTest   # 运行单个测试类
mvn verify                                   # 完整构建（含测试和检查）
```

### 前端

```bash
cd frontend
pnpm install                                 # 安装依赖
pnpm -F @vben/web-antd run dev               # 启动前端开发服务器 (:5173)
pnpm -F @vben/web-antd run build             # 构建前端
pnpm -F @vben/web-antd run typecheck         # TypeScript 类型检查
pnpm lint                                    # 全量 lint（oxlint + eslint + stylelint）
pnpm format                                  # 格式化代码
```

### Docker Compose 联调

```bash
cp .env.example .env                         # 配置 MySQL/Redis 连接
docker compose up -d --build                 # 启动全部服务
docker compose down                          # 停止并保留数据卷
docker compose down -v                       # 清理数据卷（开发库和Redis）
```

服务入口：前端 `:5173`，API `:8080`，OpenResty 节点1 `:18080`，节点2 `:28080`，Swagger `:8080/swagger-ui.html`

### Host 网络模式（动态端口）

```bash
docker compose -f docker-compose.yaml -f docker-compose.host-network.yaml \
  up -d --build control-plane openresty-east-1
```

## 架构概览

### 后端包结构 (`backend/src/main/java/net/daoke/openrestyplus/`)

| 包 | 职责 |
|---|---|
| `center/` | 中心（Center）实体与仓库 |
| `node/` | Nginx 节点实例（NginxNode），含协议、host、Control API URL |
| `httpconfig/` | HTTP Upstream、Server、Location 及其 Target |
| `streamconfig/` | Stream (TCP/UDP) Upstream 和 Server |
| `policy/` | IP 访问策略和 API 访问策略，含启用状态和绑定 |
| `tls/` | TLS 证书管理 |
| `dns/` | DNS Resolver 配置 |
| `nativeconfig/` | 将数据库模型渲染为完整 Nginx include 树，输出到文件系统 |
| `runtime/` | 发布不可变运行时 JSON 快照（供 Lua worker 轮询） |
| `release/` | 编排发布：发布 runtime → materialize native → reload 节点 |
| `reload/` | 通过 Control API PATCH 执行节点 reload |
| `task/` | Redis Streams 异步任务入队（发布/回滚等） |
| `audit/` | 操作审计日志，记录成功/失败事件 |
| `config/` | Spring 配置：Security（local/prod 分离）、Redis |
| `health/` | 共享枚举 `HealthCheckType`（TCP/HTTP/PING） |
| `web/` | 通用 DTO，如 `PageResult<T>` |

### 关键设计决策

1. **无 DTO 层**：Controller 直接用实体字段构造 record 作为响应；请求体用 `@RequestBody` record（`record Create(...)`），避免额外 DTO 类膨胀。
2. **UUID 存储**：所有实体 ID 用 `BINARY(16)` + `@JdbcTypeCode(Types.BINARY)` 存储，JPA 映射 `java.util.UUID`。
3. **JSON 列**：需要存数组/结构的字段用 `@JdbcTypeCode(Types.JSON)` + `List<String>` / `JsonNode`。
4. **Flyway 是唯一 schema 来源**：`ddl-auto=validate`，禁止修改 Hibernate 自动建表行为；新字段必须写新的 migration SQL 文件。
5. **审计通过 `AuditService`**：所有写操作（create/update/delete）必须调用 `audit.success/failure()` 记录；审计不拷贝请求 payload，只记录操作语义和结果。
6. **发布三阶段**：`RuntimeConfigurationController.publish()` 生成 JSON 快照 → `NativeConfigurationRenderer.materialize()` 原子写入文件系统 → `ControlApiReloadService.reloadCenter()` 并行 reload 各节点。生产禁止回退到 `nginx -s reload`。
7. **Active Record 模式**：实体提供 `apply(...)` 方法原地更新字段，Controller 调 `save(entity)` 持久化。

### 前端结构 (`frontend/apps/web-antd/`)

- **框架**：Vue 3 + TypeScript + Ant Design Vue 4.x，基于 Vben Admin v5.7.0 tag
- **路径别名**：`#/` 指向 `src/`，即 `#/views/...`、`#/utils/...`
- **路由**：在 `src/router/routes/modules/` 下按功能模块定义路由，`openresty.ts` 包含所有运营管理页面
- **分页**：使用 `#/utils/server-pagination.ts` 的 `useServerPagination()` composable，页码从 0 开始，返回 `PageResult<T>` 格式
- **HTTP 请求**：页面直接用 `fetch()` 调用 `/api/...`，响应 `!r.ok` 时解析 JSON 错误消息
- **页面组织**：`src/views/openresty/` 下按业务域分目录，每域一个 `index.vue`

### 数据库迁移顺序

V1（center、audit_event）→ V2（nginx_node）→ V3（http_upstream）→ V4（access_policies）→ V5（http_server/location）→ V6（runtime_configuration_versions）→ V7（audit center_id）→ V8（stream_config）→ V9（reload_tasks）→ V10（dictionary）→ V11（upstream_targets）→ V12（upstream_health_check）→ V13（location_rate_limit）→ V14（tls_certificates）→ V15（dns_resolver）→ V16（dynamic_dns）→ V17（policy_activation）→ V18（policy_targets default off）→ V19（policy_mode_order）→ V20（static_return_security）→ V21（health_check_types）

## API 关键端点

- `GET /api/centers` — 列表
- `POST /api/centers/{id}/deployments` — 触发完整发布（runtime + native + reload）
- `POST /api/centers/{id}/runtime-configurations` — 仅发布运行时快照
- `GET /api/centers/{id}/runtime-configurations/current` — 读取当前活动版本
- `POST /api/centers/{id}/runtime-configurations/{version}/rollback` — 回滚
- `GET /api/centers/{id}/native-configurations/preview` — 预览原生配置树
- `POST /api/centers/{id}/native-configurations/materialize` — 原子写入渲染目录
- `POST /api/centers/{id}/reload` — 对中心内已启用节点执行 reload
- `GET /api/dictionaries/{type}` — 字典下拉项
- `GET /api/tasks/{taskId}` — 查询异步任务状态

## 安全配置

- `local` profile（`@Profile("local")`）：完全放开认证，供本地开发联调
- 生产 profile（`@Profile("!local")`）：OAuth2 JWT 资源服务器，仅 `/actuator/health`、`/v3/api-docs/**`、`/swagger-ui/**` 免认证
- 真实凭据通过环境变量注入，禁止写入源码或默认配置

## 领域术语

- **中心（Center）**：配置发布边界，一组节点的集合
- **节点实例（Node Instance）**：中心内承载 OpenResty 的主机/容器
- **监听器（Listener）**：节点上的协议+端口+业务标识
- **配置制品（Artifact）**：由 Git commit + 中心变量 + 策略数据组成的不可变发布对象
- **策略模块（Policy Module）**：独立管理的 IP/API/限速策略，默认关闭
- **活动版本（Active Version）**：节点当前实际运行的配置制品版本

详细术语见 `CONTEXT.md`，ADR 见 `docs/adr/`。
