# ADR-0007：MVP 使用 Redis Streams 执行异步任务

## 状态

已接受

## 决策

发布、校验和回滚任务使用 Redis Streams Consumer Group；任务结果和审计持久化到 MySQL。

## 选择理由

- 复用已确定的 Redis 基础设施。
- 支持消费确认、Pending 消息恢复和有限重试。
- MVP 不额外引入 Kafka 或 RabbitMQ 运维成本。

## 约束

任务必须具备幂等 ID、中心锁和状态机；Redis 丢失不能导致 MySQL 审计和最终状态不可恢复。
