<script lang="ts" setup>
import { computed, h, onBeforeUnmount, onMounted, render } from 'vue';

import { useAntdDesignTokens } from '@vben/hooks';
import { preferences, usePreferences } from '@vben/preferences';

import { App, ConfigProvider, theme } from 'ant-design-vue';
import { QuestionCircleOutlined } from '@ant-design/icons-vue';

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
    const message = alert.textContent?.replace(/\s+/g, ' ').trim();
    if (message) alert.dataset.help = message;
    alert.setAttribute('aria-label', message || '帮助说明');
    const icon = document.createElement('span');
    icon.className = 'ops-help-icon';
    render(h(QuestionCircleOutlined), icon);
    alert.append(icon);
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
});
onBeforeUnmount(() => { drawerObserver?.disconnect(); });
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
.ops-page .ant-alert { position: relative; width: 30px; min-height: 30px; padding: 0; overflow: visible; cursor: help; border: 1px solid var(--ant-color-primary-border); border-radius: 8px; background: var(--ant-color-primary-bg); box-shadow: 0 2px 6px rgb(0 0 0 / 12%); transition: all .2s ease; }
.ops-page .ant-alert .ops-help-icon { display: grid; width: 28px; height: 28px; place-items: center; color: var(--ant-color-primary); font-size: 16px; }
.ops-page .ant-alert > *:not(.ops-help-icon) { display: none; }
.ops-page .ant-alert::after { position: absolute; z-index: 20; top: calc(100% + 8px); left: 0; width: max-content; max-width: min(420px, calc(100vw - 48px)); padding: 9px 12px; content: attr(data-help); pointer-events: none; color: var(--ant-color-text); font-size: 13px; font-weight: 400; line-height: 1.55; text-align: left; white-space: normal; border: 1px solid var(--ant-color-border-secondary); border-radius: 8px; background: var(--ant-color-bg-elevated); box-shadow: 0 8px 24px rgb(0 0 0 / 18%); opacity: 0; transform: translateY(-4px); transition: opacity .16s ease, transform .16s ease; }
.ops-page .ant-alert:hover { border-color: var(--ant-color-primary); background: var(--ant-color-primary-bg); transform: translateY(-1px); }
.ops-page .ant-alert:hover::after, .ops-page .ant-alert:focus-visible::after { opacity: 1; transform: translateY(0); }
.ops-page > .ant-card { position: relative; }
.ops-page > .ant-card > .ant-alert { position: absolute; z-index: 3; top: 20px; right: 24px; margin: 0 !important; }
.ops-page > .ant-card > .ant-alert::after { top: calc(100% + 8px); right: 0; left: auto; }
</style>
