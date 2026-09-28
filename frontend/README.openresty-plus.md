# OpenResty Plus Console

Based on official Vben Admin v5.7.0 `@vben/web-antd` at the pinned upstream commit.

Use Node 22.18.0 LTS and pnpm 10.33.4. Run `pnpm install` and `pnpm -F @vben/web-antd run dev` from this directory.

开发服务器默认通过 `http://127.0.0.1:8081` 调用 Go 控制面，已覆盖中心、节点、节点指标、HTTP/Stream、TLS、DNS 与审计查询；版本发布、原生配置、策略与实时日志尚未迁移，相关页面当前不可用。
