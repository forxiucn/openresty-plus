# 节点部署与发布脚本

当前 Compose 提供一个联调节点 `openresty-east-1`：业务/健康端点为 18080，Control API HTTP 转发端口为 18081。节点在自身启动并确认本地 Control API 转发端口可用后，通过 Go 控制面的 REST 接口登记；不再依赖独立的注册服务。节点名称、地址和端口由 `NODE_NAME`、`NODE_HOST`、`NODE_SERVICE_PORT`、`NODE_CONTROL_API_URL` 显式配置。Filebeat 采集节点日志并发送至 Kafka，但 Go 控制面尚未消费该日志流。

Compose 同时提供本地 MySQL、Redis 与 Kafka。若需要把旧外部 MySQL 的数据导入本地
MySQL，设置 `MIGRATION_SOURCE_DB_*`（或保留旧的 `OPENRESTY_DB_*`）后运行
`./deploy/scripts/migrate-external-mysql-to-compose.sh`。该脚本只读源库，导入完成后逐表
校验行数，并在目标库已有表时拒绝覆盖。脚本会检查源库非空、导出文件包含每张表的建表语句，
并逐个确认目标表已创建，避免空导出或漏表时误报迁移成功。

原生配置物化、节点 reload 与完整发布编排尚未迁移到 Go 后端，当前不可用。

生产节点上的脚本必须由 root 持有、不可由 SSH 发布用户修改，并通过 `ForceCommand` 或受限 sudo 仅暴露固定动作。

脚本职责：校验制品摘要、解包到版本目录、运行 `nginx -t`、原子切换活动目录，并通过本机 Unix socket 调用 Nginx Control API 的 REST reload 接口。

默认拒绝任意 shell、任意路径、未校验参数和 Control API 不可用时的信号回退。
