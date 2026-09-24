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
onMounted(() => {
  installDrawerResizeHandles();
  drawerObserver = new MutationObserver(installDrawerResizeHandles);
  drawerObserver.observe(document.body, { childList: true, subtree: true });
});
onBeforeUnmount(() => drawerObserver?.disconnect());
</script>

<template>
  <ConfigProvider :locale="antdLocale" :theme="tokenTheme">
    <App>
      <RouterView />
    </App>
  </ConfigProvider>
</template>

<style>
.global-drawer-resize-handle { position: absolute; z-index: 10; top: 0; bottom: 0; left: -4px; width: 9px; cursor: ew-resize; touch-action: none; }
.global-drawer-resize-handle:hover { background: color-mix(in srgb, var(--ant-color-primary) 35%, transparent); }
</style>
