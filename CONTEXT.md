# OpenResty Plus 领域词汇

## 领域边界

本项目管理多中心 OpenResty/Nginx 配置的版本、策略和发布状态。Git 保存配置期望状态；Spring Boot 控制面负责校验、审计和发布编排；管理 Web 提供可视化操作；节点本机固定脚本执行受限发布动作。

## 术语

### 中心（Center）

一组具有共同配置发布边界和运维归属的 OpenResty/Nginx 节点集合。中心拥有自己的配置合成结果、发布版本和访问策略作用域。

### 节点实例（Node Instance）

中心内承载 OpenResty/Nginx 数据面的具体主机或实例。节点实例具有协议监听器、能力基线、活动制品版本和运行状态。

### 监听器（Listener）

节点实例上一个明确的协议、监听地址/端口及其业务标识。HTTP 监听器可关联域名和 location；Stream 监听器关联 TCP/UDP 服务和端口。

### 配置制品（Configuration Artifact）

由固定 Git commit、中心变量、策略数据、校验基线和签名摘要组成的不可变发布对象。

### 策略模块（Policy Module）

独立管理和启停的 IP 访问策略、API 访问策略或限速策略。策略模块默认关闭，只有模块启用且绑定到适用资源并成功发布后才生效。

### 资源绑定（Policy Binding）

将策略模块关联到中心、监听器、HTTP 域名端口或 location/API 的明确关系。绑定拥有独立启用状态和活动发布版本。

### 发布（Deployment）

将一个配置制品按中心和节点批次执行校验、原子切换、reload 和健康确认的过程。

### 活动版本（Active Version）

节点当前实际运行并经 reload 确认的配置制品版本，不等同于 Git 分支最新版本。

### 配置 JSON

控制面、策略文件和前后端 API 中的 JSON 统一使用 Jackson 进行序列化和反序列化；不使用 Fastjson。

### Web 配置生效

Web 中的配置保存先生成结构化 Git 变更，再经过校验和发布；节点 reload 与健康检查成功后才称为生效。数据库保存的是操作和状态索引，不能绕过 Git 直接修改活动配置。

### Git 变更流程

开发/测试环境允许 Web 将结构化变更提交到受控配置分支并自动发布；生产环境通过变更分支和 Pull Request 审批，合并固定 commit 后再发布。

### Git 平台

控制面通过 Git 服务抽象访问仓库；开发环境可使用本地 bare Git，生产环境对接 GitLab/GitHub Enterprise 等托管平台，PR/MR 能力由独立适配器提供。

### MVP 范围

首个可运行版本包含中心/节点/监听器资源管理、配置浏览与版本差异、IP/API 策略模块及资源绑定启停、测试环境自动发布、受限 SSH 节点发布、Control API reload、逐节点回滚和完整审计。限速先完成模型、页面、校验和 dry-run 观察能力。

### 开发依赖凭据

MySQL 和 Redis 仅作为外置开发依赖通过环境变量或本地未提交配置注入。真实密码不得写入源码、默认配置、前端资源、日志或 Git 文档；仓库只提供变量名示例。

### 工程命名

仓库采用 `openresty-plus` 命名；后端目录为 `backend`，前端目录为 `frontend`，脚本目录为 `deploy`，配置样例目录为 `config-repo`。后端应用名为 `openresty-plus-control-plane`，前端应用名为 `openresty-plus-console`，完整 Java 基础包名为 `net.daoke.openrestyplus`。

前端固定使用官方 Vben Admin `v5.7.0` tag，对应 Node 22.18.0 LTS 和 pnpm 10.33.4；不将固定 tag 与官方 `main` 的工具版本混用。

### 后端构建

后端使用 Maven 构建，Spring Boot 依赖、Flyway 迁移、质量检查和镜像构建统一纳入 Maven 生命周期。

### 后端版本基线

后端采用 Spring Boot 4.1.1、Java 21 LTS 和 Maven 3.9+，依赖 Jakarta 命名空间及 Spring Boot 4 兼容版本。

### API 契约

控制面提供 REST API 和 OpenAPI 3.1 文档；前端 TypeScript DTO/API client 从固定版本的 OpenAPI 文档生成，异步任务统一返回任务 ID 并通过状态接口查询。

### 异步任务

发布、校验和回滚任务通过 Redis Streams Consumer Group 执行；Redis 负责短期调度和待处理恢复，MySQL 保存最终任务状态、发布结果和审计证据。

### 数据库迁移

Flyway SQL 是 MySQL schema 的唯一来源；JPA 只负责运行时映射，Hibernate 使用 `ddl-auto=validate`，禁止自动建表或修改结构。

### 节点发布通道

配置制品通过 rsync over SSH 传输；控制面只能调用受限 SSH 固定脚本。节点脚本负责校验、原子切换、nginx -t、通过本机 Unix socket 调用 Control API reload 和健康检查，不接受任意 shell 命令。

### Control API 发布硬依赖

生产节点必须具备可用的 Nginx Control REST API 和本机 Unix socket。Control API 不可用时发布失败或进入未知状态，不自动回退到 `nginx -s reload`。
