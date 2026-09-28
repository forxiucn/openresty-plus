<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Badge as ABadge, Button as AButton, Dropdown as ADropdown, Menu as AMenu, MenuItem as AMenuItem } from 'ant-design-vue';

type Center = { id: string; name: string; code: string };
type Draft = { changeCount: number };
const router = useRouter();
const centers = ref<Center[]>([]);
const pending = ref<Record<string, number>>({});
const total = computed(() => Object.values(pending.value).reduce((sum, count) => sum + count, 0));
const activeCenters = computed(() => centers.value.filter((center) => pending.value[center.id]));
let timer: number | undefined;

async function refresh() {
  try {
    centers.value = await fetch('/api/centers').then((response) => response.json());
    const items = await Promise.all(centers.value.map(async (center) => {
      const response = await fetch(`/api/centers/${center.id}/runtime-configurations/draft`);
      if (!response.ok) return [center.id, pending.value[center.id] || 0] as const;
      const draft = await response.json() as Draft;
      const locallySaved = sessionStorage.getItem(`openresty-pending:${center.id}`) === '1';
      return [center.id, locallySaved ? Math.max(1, draft.changeCount) : draft.changeCount] as const;
    }));
    pending.value = Object.fromEntries(items);
  } catch { /* keep the prior indicator while the API is unavailable */ }
}
function markPending(event: Event) {
  const centerId = (event as CustomEvent<{ centerId?: string }>).detail?.centerId;
  if (centerId) {
    pending.value = { ...pending.value, [centerId]: Math.max(1, pending.value[centerId] || 0) };
    // 保留一次保存结果，避免父页面刷新中心列表时把刚产生的提示清掉。
    sessionStorage.setItem(`openresty-pending:${centerId}`, '1');
  }
}
function open(centerId?: string) { router.push({ path: '/openresty/runtime-configurations', query: { ...(centerId ? { centerId } : {}), compare: '1' } }); }
function clearPending() { Object.keys(sessionStorage).filter((key) => key.startsWith('openresty-pending:')).forEach((key) => sessionStorage.removeItem(key)); refresh(); }
onMounted(() => { refresh(); timer = window.setInterval(refresh, 15_000); window.addEventListener('openresty-config-saved', markPending); window.addEventListener('openresty-config-published', clearPending); });
onBeforeUnmount(() => { if (timer) window.clearInterval(timer); window.removeEventListener('openresty-config-saved', markPending); window.removeEventListener('openresty-config-published', clearPending); });
</script>

<template>
  <div class="pending-publish-wrapper">
  <a-dropdown v-if="activeCenters.length > 1" placement="bottomRight">
    <a-badge :count="total" :offset="[-2, 3]" :show-zero="false"><a-button class="pending-publish" :type="total ? 'primary' : 'default'" @click="!total && open()">待发布配置</a-button></a-badge>
    <template #overlay><a-menu><a-menu-item v-for="center in activeCenters" :key="center.id" @click="open(center.id)">{{ center.name }}（{{ center.code }}） · {{ pending[center.id] }} 项变更</a-menu-item></a-menu></template>
  </a-dropdown>
  <a-badge v-else :count="total" :offset="[-2, 3]" :show-zero="false"><a-button class="pending-publish" :type="total ? 'primary' : 'default'" :disabled="!total" @click="open(activeCenters[0]?.id)">{{ total ? '待发布配置' : '配置已发布' }}</a-button></a-badge>
  </div>
</template>

<style scoped>
.pending-publish {
  min-width: 116px;
  height: 36px;
  padding: 0 18px;
  border-radius: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
  white-space: nowrap;
}
.pending-publish-wrapper {
  display: inline-flex;
  align-items: center;
  margin-right: 20px;
}
:deep(.ant-badge) {
  display: inline-flex;
  align-items: center;
}
</style>
