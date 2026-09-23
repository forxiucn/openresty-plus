# ADR-0008：Flyway SQL 作为数据库结构唯一来源

## 状态

已接受

## 决策

所有 MySQL 表结构通过不可变 Flyway SQL 迁移脚本管理；JPA/Hibernate 启动时只校验结构。

## 结果

禁止 `create`、`create-drop` 和 `update`。数据库结构变更必须新增迁移版本并经过升级验证；API 不直接暴露 JPA 实体。
