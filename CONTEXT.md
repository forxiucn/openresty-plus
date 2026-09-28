# OpenResty Plus 领域词汇

## 领域边界

本项目管理多中心 OpenResty/Nginx 配置的版本、策略和发布状态。MySQL 保存结构化配置、不可变版本快照和审计；当前 Compose 使用 Go 控制面处理已迁移的资源管理 API；管理 Web 提供可视化操作；OpenResty Lua 负责已发布运行时快照，Control API 负责原生配置 reload；Filebeat 将节点日志发送到 Kafka。

## 术语

### 中心（Center）

一组具有共同配置发布边界和运维归属的 OpenResty/Nginx 节点集合。中心拥有自己的配置合成结果、发布版本和访问策略作用域。

### 节点实例（Node Instance）

中心内承载 OpenResty/Nginx 数据面的具体主机或实例。节点实例具有协议监听器、能力基线、活动制品版本和运行状态。

### 监听器（Listener）

节点实例上一个明确的协议、监听地址/端口及其业务标识。HTTP 监听器可关联域名和 location；Stream 监听器关联 TCP/UDP 服务和端口。

### 配置制品（Configuration Artifact）

由 MySQL 版本快照、中心变量、策略数据、渲染校验基线和内容摘要组成的不可变发布对象。

### 策略模块（Policy Module）

独立管理和启停的 IP 访问策略、API 访问策略或限速策略。策略模块默认关闭，只有模块启用且绑定到适用资源并成功发布后才生效。

### 资源绑定（Policy Binding）

将策略模块关联到中心、监听器、HTTP 域名端口或 location/API 的明确关系。绑定拥有独立启用状态和活动发布版本。

### 发布（Deployment）

将一个配置制品按中心和节点批次执行校验、原子切换、reload 和健康确认的过程。

### 活动版本（Active Version）

节点当前实际运行并经 reload 确认的配置制品版本，不等同于数据库中尚未发布的编辑状态。

### 配置 JSON

控制面、策略文件和前后端 API 中的 JSON 统一使用 Go 标准库 `encoding/json` 进行序列化和反序列化。

### Web 配置生效

Web 保存只更新 MySQL。发布运行时配置后，OpenResty Lua 默认每 5 秒轮询新版本；监听端口和原生 HTTP/Stream 指令需要额外生成配置并执行 Control API reload。完整发布使用中心 deployment 接口，成功以版本、逐节点 reload 结果和审计记录共同判定。

### MVP 范围

当前 Go Compose 可运行版本包含中心、节点、节点指标、HTTP/Stream、TLS、DNS Resolver 和审计查询等资源管理 API。配置版本、差异、原生配置渲染、Control API reload、完整发布、策略资源管理及 Kafka/SSE 日志尚未迁移；这些 API 当前不可用。

### 开发依赖凭据

MySQL 和 Redis 仅作为外置开发依赖通过环境变量或本地未提交配置注入。真实密码不得写入源码、默认配置、前端资源、日志或 Git 文档；仓库只提供变量名示例。

### 工程命名

仓库采用 `openresty-plus` 命名；后端目录为 `backend`，前端目录为 `frontend`，脚本目录为 `deploy`，运行时渲染目录为 `runtime/native-config`。Go 控制面服务名为 `openresty-plus-control-plane`。

前端固定使用官方 Vben Admin `v5.7.0` tag，对应 Node 22.18.0 LTS 和 pnpm 10.33.4；不将固定 tag 与官方 `main` 的工具版本混用。

### 后端构建

Go 控制面使用 Go 1.26、`go test ./...` 和 `go vet ./...`；Docker 镜像由 `backend/Dockerfile` 构建。

### 后端版本基线

当前运行后端采用 Go 1.26。

### API 契约

控制面提供 REST API 和 OpenAPI 3.1 文档；前端 TypeScript DTO/API client 从固定版本的 OpenAPI 文档生成，异步任务统一返回任务 ID 并通过状态接口查询。

### 异步任务

发布和 reload 编排尚未迁移到 Go 控制面，当前不可用。Redis 是外部可选依赖，不是配置权威来源；MySQL 保存版本、节点结果和审计证据。

### 数据库迁移

版本化 SQL 是 MySQL schema 的唯一来源；控制面禁止自动建表或修改结构。

### 节点发布通道

当前联调节点通过项目目录绑定挂载读取控制面生成目录。节点本机 Control API 仍使用 Unix Socket，控制面通过节点登记的 HTTP 转发地址调用 reload；生产环境可继续采用受限 SSH/rsync，但不属于当前 Compose 联调闭环。

### 实例日志通道

节点日志不由控制面直接读取作为主链路。每个节点配套 Filebeat，采集 `/var/log/nginx/*.access.log` 和 `/var/log/nginx/*.error.log`，写入 Kafka topic `dtl-602-openresty-plus`。事件必须携带 `openresty.node_name` 和 `openresty_log_type`，控制面按节点缓存最近事件并提供 SSE；文件读取接口只作为故障排查备用接口。

### Control API 发布硬依赖

生产节点必须具备可用的 Nginx Control REST API 和本机 Unix socket。Control API 不可用时发布失败或进入未知状态，不自动回退到 `nginx -s reload`。
