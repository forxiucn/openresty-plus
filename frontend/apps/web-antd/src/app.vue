<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted } from 'vue';

import { useAntdDesignTokens } from '@vben/hooks';
import { preferences, usePreferences } from '@vben/preferences';

import { App, ConfigProvider, theme } from 'ant-design-vue';

import { antdLocale } from '#/locales';

defineOptions({ name: 'App' });

const { isDark } = usePreferences();
const { tokens } = useAntdDesignTokens();

const tokenTheme = computed(() => {
  const algorithm = isDark.value
    ? [theme.darkAlgorithm]
    : [theme.defaultAlgorithm];

  // antd 紧凑模式算法
  if (preferences.app.compact) {
    algorithm.push(theme.compactAlgorithm);
  }

  return {
    algorithm,
    token: tokens,
  };
});

let drawerObserver: MutationObserver | undefined;
const toggleHelpAlert = (event: MouseEvent) => {
  const target = event.target as HTMLElement | null;
  const alert = target?.closest<HTMLElement>('.ops-page .ant-alert');
  if (!alert || target?.closest('a,button,input,textarea,select,.ant-select')) return;
  alert.classList.toggle('ops-help-expanded');
};
function installDrawerResizeHandles() {
  document.querySelectorAll<HTMLElement>('.ant-drawer-content-wrapper').forEach((wrapper) => {
    if (wrapper.dataset.resizable === 'true') return;
    wrapper.dataset.resizable = 'true';
    const handle = document.createElement('div');
    handle.className = 'global-drawer-resize-handle';
    handle.addEventListener('pointerdown', (event) => {
      event.preventDefault();
      const startX = event.clientX;
      const startWidth = wrapper.getBoundingClientRect().width;
      const move = (moveEvent: PointerEvent) => {
        const width = Math.min(window.innerWidth - 48, Math.max(360, startWidth + startX - moveEvent.clientX));
        wrapper.style.width = `${width}px`;
        wrapper.style.transition = 'none';
      };
      const stop = () => {
        window.removeEventListener('pointermove', move);
        window.removeEventListener('pointerup', stop);
        wrapper.style.removeProperty('transition');
      };
      window.addEventListener('pointermove', move);
      window.addEventListener('pointerup', stop, { once: true });
    });
    wrapper.append(handle);
  });
}
function installHelpHints() {
  document.querySelectorAll<HTMLElement>('.ops-page .ant-alert').forEach((alert) => {
    if (alert.dataset.helpReady === 'true') return;
    alert.dataset.helpReady = 'true';
    alert.setAttribute('title', '点击查看说明');
  });
}
onMounted(() => {
  installDrawerResizeHandles();
  installHelpHints();
  drawerObserver = new MutationObserver(() => {
    installDrawerResizeHandles();
    installHelpHints();
  });
  drawerObserver.observe(document.body, { childList: true, subtree: true });
  document.addEventListener('click', toggleHelpAlert);
});
onBeforeUnmount(() => { drawerObserver?.disconnect(); document.removeEventListener('click', toggleHelpAlert); });
</script>

<template>
  <ConfigProvider :locale="antdLocale" :theme="tokenTheme">
    <App>
      <RouterView />
    </App>
  </ConfigProvider>
</template>

<style>
.global-drawer-resize-handle { position: absolute; z-index: 1100; top: 0; bottom: 0; left: 0; width: 12px; cursor: ew-resize; touch-action: none; pointer-events: auto; border-left: 2px solid transparent; }
.global-drawer-resize-handle::after { position: absolute; top: 50%; left: 2px; width: 4px; height: 42px; border-radius: 4px; background: var(--ant-color-border); content: ''; transform: translateY(-50%); opacity: .75; }
.global-drawer-resize-handle:hover { border-left-color: var(--ant-color-primary); background: color-mix(in srgb, var(--ant-color-primary) 12%, transparent); }
.global-drawer-resize-handle:hover::after { background: var(--ant-color-primary); opacity: 1; }
.ops-page .ant-alert:not(.ops-help-expanded) { width: 30px; min-height: 30px; padding: 0; overflow: hidden; cursor: pointer; border: 1px solid var(--ant-color-primary-border); border-radius: 8px; background: var(--ant-color-primary-bg); box-shadow: 0 2px 6px rgb(0 0 0 / 12%); transition: all .2s ease; }
.ops-page .ant-alert:not(.ops-help-expanded)::before { display: grid; width: 28px; height: 28px; place-items: center; content: 'ⓘ'; color: var(--ant-color-primary); font-size: 16px; font-weight: 600; }
.ops-page .ant-alert:not(.ops-help-expanded) > * { display: none; }
.ops-page .ant-alert:not(.ops-help-expanded):hover { border-color: var(--ant-color-primary); background: var(--ant-color-primary-bg); transform: translateY(-1px); }
.ops-page .ant-alert.ops-help-expanded { max-width: 100%; margin: 10px 0 14px; cursor: pointer; border-radius: 8px; box-shadow: 0 4px 14px rgb(0 0 0 / 10%); }
.ops-page .ant-alert.ops-help-expanded .ant-alert-message { font-weight: 600; }
.ops-page .ant-alert.ops-help-expanded .ant-alert-description { line-height: 1.65; }
</style>
