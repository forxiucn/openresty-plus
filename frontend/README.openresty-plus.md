# OpenResty Plus Console

Based on official Vben Admin v5.7.0 `@vben/web-antd` at the pinned upstream commit.

Use Node 22.18.0 LTS and pnpm 10.33.4. Run `pnpm install` and `pnpm -F @vben/web-antd run dev` from this directory.

开发服务器默认通过 `http://127.0.0.1:8080` 调用控制面。当前 Compose 使用 Go 控制面，已覆盖中心、节点、HTTP/Stream、TLS、DNS 与审计查询；版本发布、原生配置、策略、节点指标与实时日志尚未迁移到 Go，相关页面在 Go Compose 模式下不能视为可用功能。
