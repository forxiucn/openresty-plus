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
    const items = await Promise.all(centers.value.map(async (center) => [center.id, await fetch(`/api/centers/${center.id}/runtime-configurations/draft`).then((response) => response.ok ? response.json() : { changeCount: 0 })] as const));
    pending.value = Object.fromEntries(items.map(([id, draft]) => [id, (draft as Draft).changeCount]));
  } catch { /* keep the prior indicator while the API is unavailable */ }
}
function open(centerId?: string) { router.push({ path: '/openresty/runtime-configurations', query: { ...(centerId ? { centerId } : {}), compare: '1' } }); }
onMounted(() => { refresh(); timer = window.setInterval(refresh, 15_000); window.addEventListener('openresty-config-saved', refresh); window.addEventListener('openresty-config-published', refresh); });
onBeforeUnmount(() => { if (timer) window.clearInterval(timer); window.removeEventListener('openresty-config-saved', refresh); window.removeEventListener('openresty-config-published', refresh); });
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
