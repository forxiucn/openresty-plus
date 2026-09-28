import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            // 本地开发与 Compose 保持一致，统一通过 /api 访问 Go 控制面。
            target: 'http://127.0.0.1:8081',
            ws: true,
          },
        },
      },
    },
  };
});
