# ADR-0002：统一使用 Jackson 处理 JSON

## 状态

已接受

## 决策

Spring Boot 后端所有 JSON 序列化、反序列化、策略 DTO 转换和 API 数据处理统一使用 Jackson，不引入 Fastjson。

## 背景

平台处理配置策略、发布任务和审计数据，既需要严格 schema 校验，也需要避免多个 JSON 库带来的行为差异和依赖治理成本。

## 选择理由

- 与 Spring Boot 默认 Web 栈集成，减少重复配置。
- 支持显式 DTO、字段约束、未知字段策略和统一时间格式。
- 便于集中执行反序列化安全配置和依赖升级。

## 实施约束

- 禁止在业务代码中引入 Fastjson 依赖或直接调用其 API。
- API 使用显式请求/响应 DTO，不直接暴露 JPA 实体。
- 对策略输入启用 schema/Bean Validation，并根据接口契约决定未知字段是否拒绝。
