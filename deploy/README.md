# 节点部署与发布脚本

当前 Compose 提供一个显式登记的联调节点 `openresty-east-1`：业务/健康端点为 18080，Control API HTTP 转发端口为 18081。`openresty-east-2` 已从 Compose 中移除，不会被节点注册脚本重新创建。Filebeat 采集节点日志并发送至 Kafka，但 Go 控制面尚未消费该日志流。

原生配置物化、节点 reload 与完整发布编排尚未迁移到 Go 后端，当前不可用。

生产节点上的脚本必须由 root 持有、不可由 SSH 发布用户修改，并通过 `ForceCommand` 或受限 sudo 仅暴露固定动作。

脚本职责：校验制品摘要、解包到版本目录、运行 `nginx -t`、原子切换活动目录，并通过本机 Unix socket 调用 Nginx Control API 的 REST reload 接口。

默认拒绝任意 shell、任意路径、未校验参数和 Control API 不可用时的信号回退。
