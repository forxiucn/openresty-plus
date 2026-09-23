# ADR-0009：使用 rsync 与受限 SSH 固定脚本发布

## 状态

已接受

## 决策

Spring Boot 通过 rsync over SSH 传输签名制品，再通过受限 SSH 调用节点预置固定脚本。脚本在节点本机完成校验、原子切换和 Control API reload。

## 结果

控制面不直连 Control API，不拥有任意远程 shell 或 root 权限。SSH 输入必须是结构化、已校验的任务和制品标识；脚本 root-owned 且只能执行白名单动作。
