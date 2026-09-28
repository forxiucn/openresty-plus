# CLAUDE.md

## 项目概述

OpenResty Plus 是多中心 OpenResty/Nginx 配置管理平台。Go 控制面负责 REST API 和 MySQL 数据访问；Vben Admin 提供管理界面；节点通过 Control API 获取运行时配置并执行 reload。

## 开发环境

- Go：1.26.1
- Node：22.18.0 LTS
- pnpm：10.33.4
- Docker Compose：本地 MySQL、Redis、Kafka 与联调节点

## 常用命令

### 控制面

```bash
cd backend
go test ./...
go run ./cmd/control-plane
```

控制面默认监听 `:8081`，数据库连接通过 `OPENRESTY_DB_*` 环境变量配置。

### 前端

```bash
cd frontend
pnpm install
pnpm -F @vben/web-antd run dev
pnpm -F @vben/web-antd run build
pnpm -F @vben/web-antd run typecheck
```

开发服务器将 `/api` 代理到 `http://127.0.0.1:8081`。

### Docker Compose 联调

```bash
cp .env.example .env
docker compose up -d --build
docker compose down
```

服务入口：前端 `:5173`、控制面 `:8081`、OpenResty 节点 `:18080`、节点 Control API 转发 `:18081`。

## 后端结构

- `backend/cmd/control-plane`：控制面入口。
- `backend/internal/config`：环境变量与数据库连接配置。
- `backend/internal/store`：MySQL 连接。
- `backend/internal/httpapi`：REST 路由、资源处理器、认证兼容接口与审计。

## 约束

- MySQL 是配置权威；Redis 和 Kafka 为本地联调依赖，不得阻断基础配置管理。
- 写操作必须记录审计事件；UUID 按 MySQL `BINARY(16)` 存储并在 API 层转换。
- 页面请求统一使用 `/api`；生产前端通过 Nginx 同源代理访问控制面。
- 保存配置不等同于节点生效；发布、原生配置物化和 reload 必须分别验证。
- 真实凭据仅通过环境变量注入，禁止写入源码、文档或前端构建产物。
