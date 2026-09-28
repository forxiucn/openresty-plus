# OpenResty Plus

面向多中心 OpenResty/Nginx 的配置管理平台。以 Center 为配置与发布边界，使用 MySQL 保存结构化配置和审计记录，Web 控制台提供 HTTP、Stream、TLS、DNS 和节点资源管理能力。

## 当前运行状态

`docker-compose.yaml` 启动 Go 控制面（`backend/`，监听 `:8081`）。

已迁移到 Go 的 REST 接口包括：

- Center、节点、HTTP 全局设置；
- HTTP Upstream 与后端实例；
- HTTP Server、Location、访问策略开关、动态 DNS、静态目录和直接返回指令；
- TLS 证书；Stream Upstream 与 Stream Server；
- DNS Resolver 与审计事件查询。

配置版本/草稿/回滚、原生配置预览与物化、Control API reload、完整发布编排、策略资源管理、字典管理、节点指标和 Kafka/SSE 日志流仍在持续完善；不能因 Go 服务健康检查通过而视为完整发布闭环已完成。

## 目录

- `backend/`：当前 Compose 使用的 Go 控制面（Go 1.26）。
- `frontend/`：Vben Admin v5.7.0 的 `@vben/web-antd` 控制台。
- `deploy/`：OpenResty、Filebeat、节点注册及发布脚本。
- `docs/`：PRD 与 ADR。
- `runtime/`：本地渲染配置与节点日志；日志不应提交 Git。

## 本地开发

1. 复制 `.env.example` 为未提交的 `.env`，填写外部 MySQL 连接信息。
2. 启动 Go 控制面：

   ```bash
   cd backend
   go test ./...
   go run ./cmd/control-plane
   ```

3. 启动前端（Node 22.18.0、pnpm 10.33.4）：

   ```bash
   cd frontend
   pnpm install
   pnpm -F @vben/web-antd run dev
   ```

尚未实现的功能当前不可用；不得以未验证的替代服务作为回退。

## Docker 联调

根目录准备 `.env` 后执行。首次从原外部 MySQL 迁移数据时，填写指向外部源库的
`MIGRATION_SOURCE_DB_*`，然后先执行：

```bash
./deploy/scripts/migrate-external-mysql-to-compose.sh
```

脚本只读取源库，导入后会逐表校验行数；为防止误覆盖，目标库已有表时会拒绝执行。
迁移完成后执行：

```bash
docker compose up -d --build
curl http://127.0.0.1:8081/healthz
```

服务入口：

- 管理 Web：<http://127.0.0.1:5173>
- Go 控制面：<http://127.0.0.1:8081>，健康检查为 `/healthz`
- OpenResty 联调节点：<http://127.0.0.1:18080/health>
- 节点 Control API HTTP 转发：<http://127.0.0.1:18081>

登录页使用 Hash 路由：<http://127.0.0.1:5173/#/auth/login>。认证接口只能以 `POST /api/auth/login` 调用；浏览器通过同源 `/api` 代理访问，或从本地开发端口直接访问控制面时会获得 CORS 预检响应。

Compose 会创建本地 MySQL 8.4、Redis 7.4 和 Kafka 3.9（KRaft 单节点），数据保存在
`mysql-data`、`redis-data`、`kafka-data` named volume 中；运行服务只连接本地实例。
渲染目录绑定到 `./runtime/native-config/`。服务使用 host 网络，端口必须与宿主机其他进程不冲突。

`openresty-east-1` 在本地业务端口和 Control API 转发端口就绪后，主动调用 Go 控制面的 REST 接口登记自身；Compose 不再启动独立注册服务。节点标识和地址通过该节点的 `NODE_*` 环境变量显式配置。`filebeat-east-1` 读取该节点的 `/var/log/nginx` 并发往 `OPENRESTY_KAFKA_*` 指定的 Kafka；Go 控制面尚未实现 Kafka 消费和 SSE 输出。

停止联调环境：

```bash
docker compose down
```

## 配置与安全边界

- MySQL 是配置权威；真实密码不得写入源码、文档、前端资源或 Git。
- 保存配置不等同于节点生效。原生配置生成、reload 与发布闭环尚未迁移到 Go 后端，当前不可用。
- 业务监听端口、健康状态端点和 Control API 是不同端口：业务端口承载流量；节点的 18080 用于健康/状态读取；18081 是 Control API 转发端口。
- 生产环境不得使用 `local` profile，也不得将节点 Control API 暴露到不受控网络。
