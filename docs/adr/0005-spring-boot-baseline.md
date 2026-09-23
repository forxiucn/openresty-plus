# ADR-0005：采用 Spring Boot 4.1.1 与 Java 21

## 状态

已接受

## 决策

控制面基于 Spring Boot 4.1.1、Java 21 LTS 和 Maven 3.9+ 开发。

## 背景

项目是新建控制面，没有旧版 Spring Boot 兼容负担，需要选择当前稳定、长期可维护的技术基线。

## 结果

所有依赖必须兼容 Spring Boot 4 与 Jakarta 命名空间。升级必须通过依赖清单和回归验证，不自动跟随最新版本。
