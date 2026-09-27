import { createApp, watchEffect } from 'vue';

import { registerAccessDirective } from '@vben/access';
import { registerLoadingDirective } from '@vben/common-ui/es/loading';
import { preferences } from '@vben/preferences';
import { initStores } from '@vben/stores';
import '@vben/styles';
import '@vben/styles/antd';
import './styles/openresty-operations.css';

import { useTitle } from '@vueuse/core';

import { $t, setupI18n } from '#/locales';

import { initComponentAdapter } from './adapter/component';
import { initSetupVbenForm } from './adapter/form';
import App from './app.vue';
import { router } from './router';

/**
 * 移除已下线页面的会话标签，避免浏览器恢复到没有路由的旧标签。
 * 仅处理本次下线的分析页和日志格式页，不影响用户的其他已打开标签。
 */
function removeRetiredSessionTabs(namespace: string) {
  const key = `${namespace}-core-tabbar`;
  const raw = sessionStorage.getItem(key);
  if (!raw) return;

  try {
    const persisted = JSON.parse(raw);
    const retired = new Set(['/analytics', '/openresty/log-formats']);
    const isRetired = (tab: { path?: string }) => retired.has(tab.path || '');
    if (!Array.isArray(persisted.tabs) || !persisted.tabs.some(isRetired)) return;

    persisted.tabs = persisted.tabs.filter((tab: { path?: string }) => !isRetired(tab));
    if (Array.isArray(persisted.visitHistory?.items)) {
      persisted.visitHistory.items = persisted.visitHistory.items.filter(
        (key: string) => ![...retired].some((path) => key.includes(path)),
      );
    }
    sessionStorage.setItem(key, JSON.stringify(persisted));
  } catch {
    // 标签缓存不完整时保留原内容，避免影响正常启动。
  }
}

async function bootstrap(namespace: string) {
  // 初始化组件适配器
  await initComponentAdapter();

  // 初始化表单组件
  await initSetupVbenForm();

  // // 设置弹窗的默认配置
  // setDefaultModalProps({
  //   fullscreenButton: false,
  // });
  // // 设置抽屉的默认配置
  // setDefaultDrawerProps({
  //   zIndex: 1020,
  // });

  const app = createApp(App);

  // 注册v-loading指令
  registerLoadingDirective(app, {
    loading: 'loading', // 在这里可以自定义指令名称，也可以明确提供false表示不注册这个指令
    spinning: 'spinning',
  });

  // 国际化 i18n 配置
  await setupI18n(app);

  removeRetiredSessionTabs(namespace);

  // 配置 pinia-tore
  await initStores(app, { namespace });

  // 安装权限指令
  registerAccessDirective(app);

  // 初始化 tippy
  const { initTippy } = await import('@vben/common-ui/es/tippy');
  initTippy(app);

  // 配置路由及路由守卫
  app.use(router);

  // 配置Motion插件
  const { MotionPlugin } = await import('@vben/plugins/motion');
  app.use(MotionPlugin);

  // 动态更新标题
  watchEffect(() => {
    if (preferences.app.dynamicTitle) {
      const routeTitle = router.currentRoute.value.meta?.title;
      const pageTitle =
        (routeTitle ? `${$t(routeTitle)} - ` : '') + preferences.app.name;
      useTitle(pageTitle);
    }
  });

  app.mount('#app');
}

export { bootstrap };
