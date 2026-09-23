<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';

import {
  Alert as AAlert,
  Button as AButton,
  Card as ACard,
  Col as ACol,
  Descriptions as ADescriptions,
  DescriptionsItem as ADescriptionsItem,
  Empty as AEmpty,
  Row as ARow,
  Select as ASelect,
  Table as ATable,
  Tag as ATag,
  message,
} from 'ant-design-vue';

type Center = { id: string; code: string; name: string; enabled: boolean };
type RuntimeVersion = {
  id: string;
  versionNo: number;
  checksum: string;
  state: string;
  createdAt: string;
};
type RuntimeContent = {
  httpUpstreams?: unknown[];
  httpServers?: unknown[];
  ipPolicies?: unknown[];
  apiPolicies?: unknown[];
  schemaVersion?: number;
};
type PublishedConfiguration = RuntimeVersion & { content: RuntimeContent; changed: boolean };
type AuditEvent = { id: string; actor: string; action: string; result: string; createdAt: string };
type ReloadNodeResult = { nodeId: string; nodeName: string; status: string; httpStatus?: number; message?: string; completedAt?: string };
type ReloadTask = { id: string; status: string; createdAt: string; completedAt?: string; nodeResults: ReloadNodeResult[] };

const centers = ref<Center[]>([]);
const versions = ref<RuntimeVersion[]>([]);
const auditEvents = ref<AuditEvent[]>([]);
const current = ref<PublishedConfiguration>();
const selectedCenterId = ref<string>();
const loading = ref(false);
const publishing = ref(false);
const reloading = ref(false);
const reloadTasks = ref<ReloadTask[]>([]);

const selectedCenter = computed(() => centers.value.find((item) => item.id === selectedCenterId.value));
const centerOptions = computed(() => centers.value.map((item) => ({ value: item.id, label: `${item.name}（${item.code}）` })));
const summary = computed(() => ({
  upstreams: current.value?.content.httpUpstreams?.length ?? 0,
  servers: current.value?.content.httpServers?.length ?? 0,
  ipPolicies: current.value?.content.ipPolicies?.length ?? 0,
  apiPolicies: current.value?.content.apiPolicies?.length ?? 0,
}));
const versionColumns = [
  { dataIndex: 'versionNo', key: 'versionNo', title: '版本' },
  { dataIndex: 'state', key: 'state', title: '状态' },
  { dataIndex: 'checksum', key: 'checksum', title: '校验值' },
  { dataIndex: 'createdAt', key: 'createdAt', title: '发布时间' },
  { key: 'actions', title: '操作', width: 110 },
];
const auditColumns = [
  { dataIndex: 'createdAt', key: 'createdAt', title: '时间' },
  { dataIndex: 'actor', key: 'actor', title: '操作人' },
  { dataIndex: 'action', key: 'action', title: '操作' },
  { dataIndex: 'result', key: 'result', title: '结果' },
];
const reloadColumns = [
  { dataIndex: 'createdAt', key: 'createdAt', title: '发起时间' },
  { dataIndex: 'status', key: 'status', title: '执行状态' },
  { dataIndex: 'nodeResults', key: 'nodeResults', title: '节点结果' },
];

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail || body.message || '请求失败');
  }
  return response.json() as Promise<T>;
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—';
}

function versionStateLabel(value: string) {
  return ({ PUBLISHED: '已发布', ROLLED_BACK: '已回滚' } as Record<string, string>)[value] || value;
}

function auditActionLabel(value: string) {
  const labels: Record<string, string> = {
    CENTER_CREATED: '创建中心', CENTER_UPDATED: '更新中心', CENTER_DELETED: '删除中心',
    NGINX_NODE_CREATED: '创建节点', NGINX_NODE_UPDATED: '更新节点', NGINX_NODE_DELETED: '删除节点',
    HTTP_UPSTREAM_CREATED: '创建 HTTP Upstream', HTTP_UPSTREAM_UPDATED: '更新 HTTP Upstream', HTTP_UPSTREAM_DELETED: '删除 HTTP Upstream',
    HTTP_SERVER_CREATED: '创建 HTTP Server', HTTP_SERVER_UPDATED: '更新 HTTP Server', HTTP_SERVER_DELETED: '删除 HTTP Server',
    HTTP_LOCATION_CREATED: '创建 HTTP Location', HTTP_LOCATION_UPDATED: '更新 HTTP Location', HTTP_LOCATION_DELETED: '删除 HTTP Location',
    IP_POLICY_CREATED: '创建 IP 策略', IP_POLICY_UPDATED: '更新 IP 策略', IP_POLICY_DELETED: '删除 IP 策略',
    API_POLICY_CREATED: '创建接口策略', API_POLICY_UPDATED: '更新接口策略', API_POLICY_DELETED: '删除接口策略',
    STREAM_UPSTREAM_CREATED: '创建四层 Upstream', STREAM_UPSTREAM_UPDATED: '更新四层 Upstream', STREAM_UPSTREAM_DELETED: '删除四层 Upstream',
    STREAM_SERVER_CREATED: '创建四层 Server', STREAM_SERVER_UPDATED: '更新四层 Server', STREAM_SERVER_DELETED: '删除四层 Server',
    RUNTIME_CONFIGURATION_PUBLISHED: '发布运行配置', RUNTIME_CONFIGURATION_ROLLED_BACK: '回滚运行配置',
    CONTROL_API_RELOAD: '执行 Control API 重载', NATIVE_CONFIGURATION_MATERIALIZED: '生成原生配置',
  };
  return labels[value] || value;
}

function reloadStateLabel(value: string) {
  return ({ SUCCESS: '全部成功', FAILED: '执行失败', PARTIAL_SUCCESS: '部分成功', RUNNING: '执行中' } as Record<string, string>)[value] || value;
}

async function loadCenters() {
  loading.value = true;
  try {
    centers.value = await request<Center[]>('/centers');
    if (!selectedCenterId.value && centers.value[0]) await selectCenter(centers.value[0].id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载中心失败');
  } finally {
    loading.value = false;
  }
}

async function selectCenter(centerId: string) {
  selectedCenterId.value = centerId;
  current.value = undefined;
  loading.value = true;
  try {
    const [loadedVersions, loadedAudits, loadedCurrent, loadedReloads] = await Promise.all([
      request<RuntimeVersion[]>(`/centers/${centerId}/runtime-configurations`),
      request<AuditEvent[]>(`/centers/${centerId}/audit-events`),
      request<PublishedConfiguration>(`/centers/${centerId}/runtime-configurations/current`).catch(() => undefined),
      request<ReloadTask[]>(`/centers/${centerId}/control-api-reloads`).catch(() => []),
    ]);
    versions.value = loadedVersions;
    auditEvents.value = loadedAudits;
    current.value = loadedCurrent;
    reloadTasks.value = loadedReloads;
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载版本信息失败');
  } finally {
    loading.value = false;
  }
}

async function reloadNativeConfiguration() {
  if (!selectedCenterId.value) return;
  reloading.value = true;
  try {
    await request(`/centers/${selectedCenterId.value}/native-configurations/materialize`, { method: 'POST' });
    const task = await request<ReloadTask>(`/centers/${selectedCenterId.value}/reload`, { method: 'POST' });
    message.success(reloadStateLabel(task.status));
    await selectCenter(selectedCenterId.value);
  } catch (error) {
    message.error(error instanceof Error ? error.message : '原生配置重载失败');
  } finally {
    reloading.value = false;
  }
}

async function publish() {
  if (!selectedCenterId.value) return;
  publishing.value = true;
  try {
    const published = await request<PublishedConfiguration>(`/centers/${selectedCenterId.value}/runtime-configurations`, { method: 'POST' });
    message.success(published.changed ? `配置版本 v${published.versionNo} 已发布` : `配置未变化，仍使用 v${published.versionNo}`);
    await selectCenter(selectedCenterId.value);
  } catch (error) {
    message.error(error instanceof Error ? error.message : '发布运行配置失败');
  } finally {
    publishing.value = false;
  }
}

async function rollback(version: RuntimeVersion) {
  if (!selectedCenterId.value) return;
  publishing.value = true;
  try {
    const restored = await request<PublishedConfiguration>(
      `/centers/${selectedCenterId.value}/runtime-configurations/${version.versionNo}/rollback`,
      { method: 'POST' },
    );
    message.success(`已从 v${version.versionNo} 创建回滚版本 v${restored.versionNo}`);
    await selectCenter(selectedCenterId.value);
  } catch (error) {
    message.error(error instanceof Error ? error.message : '回滚运行配置失败');
  } finally {
    publishing.value = false;
  }
}

onMounted(loadCenters);
</script>

<template>
  <div class="p-5">
    <a-card :bordered="false" title="运行配置版本与操作审计">
      <a-alert class="mb-4" show-icon type="info" message="发布会将当前中心的 HTTP 配置和策略固化为 MySQL 不可变快照。Lua 工作进程可按版本读取快照；监听端口等原生配置由 Control API 触发 reload。" />
      <div class="flex flex-wrap items-center gap-3">
        <span>配置中心</span>
        <a-select v-model:value="selectedCenterId" class="w-80" placeholder="请选择中心" :options="centerOptions" @change="selectCenter" />
        <a-button :disabled="!selectedCenterId" :loading="publishing" type="primary" @click="publish">发布当前配置</a-button>
        <a-button :disabled="!selectedCenterId" :loading="reloading" @click="reloadNativeConfiguration">生成原生配置并重载</a-button>
      </div>
    </a-card>

    <a-row class="mt-5" :gutter="16">
      <a-col :lg="10" :xs="24">
        <a-card :bordered="false" :loading="loading" :title="selectedCenter ? `${selectedCenter.name} 的当前快照` : '当前快照'">
          <template v-if="current">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="当前版本">v{{ current.versionNo }}</a-descriptions-item>
              <a-descriptions-item label="发布时间">{{ formatTime(current.createdAt) }}</a-descriptions-item>
              <a-descriptions-item label="校验值"><span class="break-all font-mono text-xs">{{ current.checksum }}</span></a-descriptions-item>
            </a-descriptions>
            <a-row class="mt-5" :gutter="12">
              <a-col :span="6"><div class="summary"><b>{{ summary.upstreams }}</b><span>Upstream</span></div></a-col>
              <a-col :span="6"><div class="summary"><b>{{ summary.servers }}</b><span>Server</span></div></a-col>
              <a-col :span="6"><div class="summary"><b>{{ summary.ipPolicies }}</b><span>IP 策略</span></div></a-col>
              <a-col :span="6"><div class="summary"><b>{{ summary.apiPolicies }}</b><span>API 策略</span></div></a-col>
            </a-row>
          </template>
          <a-empty v-else description="尚未发布运行配置" />
        </a-card>
      </a-col>
      <a-col :lg="14" :xs="24" class="max-lg:mt-4">
        <a-card :bordered="false" :title="selectedCenter ? `${selectedCenter.name} 的版本历史` : '版本历史'">
          <a-table :columns="versionColumns" :data-source="versions" :loading="loading" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'versionNo'">v{{ record.versionNo }}</template>
              <template v-else-if="column.key === 'state'"><a-tag :color="record.state === 'ROLLED_BACK' ? 'orange' : 'green'">{{ versionStateLabel(record.state) }}</a-tag></template>
              <template v-else-if="column.key === 'checksum'"><span class="font-mono text-xs">{{ record.checksum.slice(0, 16) }}…</span></template>
              <template v-else-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</template>
              <template v-else-if="column.key === 'actions'"><a-button size="small" :disabled="publishing || record.versionNo === current?.versionNo" @click="rollback(record)">回滚到此版本</a-button></template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="mt-5" :bordered="false" :title="selectedCenter ? `${selectedCenter.name} 的操作审计` : '操作审计'">
      <a-table :columns="auditColumns" :data-source="auditEvents" :loading="loading" :pagination="false" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</template>
          <template v-else-if="column.key === 'action'">{{ auditActionLabel(record.action) }}</template>
          <template v-else-if="column.key === 'result'"><a-tag :color="record.result === 'SUCCESS' ? 'green' : 'red'">{{ record.result === 'SUCCESS' ? '成功' : '失败' }}</a-tag></template>
        </template>
      </a-table>
    </a-card>

    <a-card class="mt-5" :bordered="false" :title="selectedCenter ? `${selectedCenter.name} 的原生配置重载记录` : '原生配置重载记录'">
      <a-table :columns="reloadColumns" :data-source="reloadTasks" :loading="loading" :pagination="false" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</template>
          <template v-else-if="column.key === 'status'"><a-tag :color="record.status === 'SUCCESS' ? 'green' : record.status === 'PARTIAL_SUCCESS' ? 'orange' : 'red'">{{ reloadStateLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'nodeResults'"><span v-if="!record.nodeResults.length">没有可重载节点</span><a-tag v-for="item in record.nodeResults" :key="item.nodeId" :color="item.status === 'SUCCESS' ? 'green' : 'red'">{{ item.nodeName }}：{{ item.status === 'SUCCESS' ? '成功' : '失败' }}{{ item.httpStatus ? ` (${item.httpStatus})` : '' }}</a-tag></template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.summary { display: flex; flex-direction: column; align-items: center; padding: 12px 4px; border-radius: 6px; background: rgb(0 0 0 / 2%); }
.summary b { font-size: 22px; line-height: 1.4; }
.summary span { color: rgb(0 0 0 / 45%); font-size: 12px; white-space: nowrap; }
</style>
