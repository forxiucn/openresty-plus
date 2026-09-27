# OpenResty Plus Java Reference Control Plane

Spring Boot 4.1.1 / Java 21 原始控制面。当前 `docker-compose.yaml` 默认启动 `../backend-go`，本目录用于保留尚未迁移到 Go 的版本、发布、原生配置渲染、Control API reload、策略、节点指标和日志流实现。

## Local run

停止 Go 控制面后，设置 `SPRING_PROFILES_ACTIVE=local` 以及 `OPENRESTY_DB_*` / `OPENRESTY_REDIS_*` 环境变量，再从本目录运行 `mvn spring-boot:run`。

The local profile disables authentication only for development. Production requires OIDC and must not use this profile.
