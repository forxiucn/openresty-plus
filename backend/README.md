# OpenResty Plus Go Control Plane

当前 Compose 使用的控制面，监听 `:8081`，通过 `OPENRESTY_DB_URL`、`OPENRESTY_DB_USERNAME` 与 `OPENRESTY_DB_PASSWORD` 连接本地 MySQL。

## 已实现接口

Center、节点、HTTP 设置、HTTP Upstream/后端实例、HTTP Server/Location、TLS、Stream、DNS Resolver 和审计事件查询。

## 尚未迁移

运行时版本、草稿差异、原生配置渲染/物化、Control API reload、部署编排、字典、IP/API 策略资源，以及 Kafka/SSE 日志链路。原 Java 实现已移除，以上能力须完成 Go 迁移后才能使用。

## 开发与检查

```bash
go test ./...
go vet ./...
go run ./cmd/control-plane
```

Docker Compose 从仓库根目录以 `backend/Dockerfile` 构建该服务。
