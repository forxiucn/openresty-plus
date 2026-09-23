# ADR-0006：采用 REST 与 OpenAPI 3.1 作为接口契约

## 状态

已接受

## 决策

Spring Boot 控制面提供 REST API，生成 OpenAPI 3.1 文档；前端请求类型和 API client 从版本化文档生成。

## 结果

异步校验、部署和回滚接口使用任务 ID 与状态资源。接口变更必须同步更新 OpenAPI、后端 DTO、前端生成代码和兼容性说明。
