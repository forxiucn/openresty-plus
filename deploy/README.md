# 节点发布脚本

生产节点上的脚本必须由 root 持有、不可由 SSH 发布用户修改，并通过 `ForceCommand` 或受限 sudo 仅暴露固定动作。

脚本职责：校验制品摘要、解包到版本目录、运行 `nginx -t`、原子切换活动目录，并通过本机 Unix socket 调用 Nginx Control API 的 REST reload 接口。

默认拒绝任意 shell、任意路径、未校验参数和 Control API 不可用时的信号回退。
