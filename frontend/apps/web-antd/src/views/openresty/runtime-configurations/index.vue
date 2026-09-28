<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import {
  Alert as AAlert,
  Button as AButton,
  Card as ACard,
  Col as ACol,
  Descriptions as ADescriptions,
  DescriptionsItem as ADescriptionsItem,
  Empty as AEmpty,
  Modal as AModal,
  Row as ARow,
  Select as ASelect,
  Table as ATable,
  Tag as ATag,
  message,
} from 'ant-design-vue';
import MetricGrid from '#/components/operations/MetricGrid.vue';
import SectionHelp from '#/components/openresty/SectionHelp.vue';
import { type PageResult, useServerPagination } from '#/utils/server-pagination';

type Center = { id: string; code: string; name: string; enabled: boolean };
type RuntimeVersion = {
  id: string;
  versionNo: number;
  checksum: string;
  state: string;
  createdAt: string;
};
type RuntimeContent = {
  httpConfiguration?: Record<string, unknown>;
  httpUpstreams?: unknown[];
  httpServers?: unknown[];
  httpLocations?: unknown[];
  ipPolicies?: unknown[];
  apiPolicies?: unknown[];
  schemaVersion?: number;
};
type PublishedConfiguration = RuntimeVersion & { content: RuntimeContent; changed: boolean };
type AuditEvent = { id: string; actor: string; action: string; result: string; createdAt: string };
type ReloadNodeResult = { nodeId: string; nodeName: string; status: string; httpStatus?: number; message?: string; completedAt?: string };
type ReloadTask = { id: string; status: string; createdAt: string; completedAt?: string; nodeResults: ReloadNodeResult[] };
type DraftStatus = { publishedVersionNo?: number; changeCount: number; changedSections: string[] };
type DraftComparison = { publishedVersionNo?: number; published?: Record<string, unknown>; draft: Record<string, unknown>; changedSections: string[] };

const centers = ref<Center[]>([]);
const versions = ref<RuntimeVersion[]>([]);
const auditEvents = ref<AuditEvent[]>([]);
const current = ref<PublishedConfiguration>();
const selectedCenterId = ref<string>();
const loading = ref(false);
const publishing = ref(false);
const reloading = ref(false);
const reloadTasks = ref<ReloadTask[]>([]);
const draft = ref<DraftStatus>();
const comparison = ref<DraftComparison>();
const comparisonOpen = ref(false);
const comparing = ref(false);
const initialComparisonRequested = ref(false);
const route = useRoute();
const versionRows = ref<RuntimeVersion[]>([]), auditRows = ref<AuditEvent[]>([]), reloadRows = ref<ReloadTask[]>([]);
const versionPager = useServerPagination(), auditPager = useServerPagination(), reloadPager = useServerPagination();

const selectedCenter = computed(() => centers.value.find((item) => item.id === selectedCenterId.value));
const centerOptions = computed(() => centers.value.map((item) => ({ value: item.id, label: `${item.name}（${item.code}）` })));
const sectionLabels: Record<string, string> = { httpConfiguration: 'HTTP 全局配置', httpUpstreams: 'HTTP Upstream', httpServers: 'HTTP Server', httpLocations: 'HTTP Location', ipPolicies: 'IP 访问策略', apiPolicies: '接口访问策略', streamUpstreams: 'Stream Upstream', streamServers: 'Stream Server' };
const comparisonRows = computed(() => (comparison.value?.changedSections || []).map((section) => ({
  key: section,
  label: sectionLabels[section] || section,
  published: comparison.value?.published?.[section],
  draft: comparison.value?.draft?.[section],
})));
type DiffEntry = { path: string; before: string; after: string; kind: 'added' | 'removed' | 'changed' };
function flatten(value: unknown, path = ''): Record<string, string> {
  if (value === null || typeof value !== 'object') return { [path || '(值)']: formatJson(value) };
  if (Array.isArray(value)) {
    if (!value.length) return { [path || '(空数组)']: '[]' };
    return value.reduce((all, item, index) => {
      const object = item && typeof item === 'object' ? item as Record<string, unknown> : undefined;
      const identity = object && (object.name || object.domain || object.service_name || object.path || object.id);
      const marker = identity ? `${String(identity)}` : String(index);
      return { ...all, ...flatten(item, `${path}[${marker}]`) };
    }, {});
  }
  const entries = Object.entries(value as Record<string, unknown>);
  if (!entries.length) return { [path || '(空对象)']: '{}' };
  return entries.reduce((all, [key, item]) => ({ ...all, ...flatten(item, path ? `${path}.${key}` : key) }), {});
}
function diffFor(item: { published: unknown; draft: unknown }): DiffEntry[] {
  const before = flatten(item.published);
  const after = flatten(item.draft);
  return [...new Set([...Object.keys(before), ...Object.keys(after)])].flatMap((path) => {
    const oldValue = before[path]; const newValue = after[path];
    if (oldValue === newValue) return [];
    return [{ path, before: oldValue ?? '—', after: newValue ?? '—', kind: oldValue === undefined ? 'added' : newValue === undefined ? 'removed' : 'changed' }];
  });
}
const summary = computed(() => ({
  upstreams: current.value?.content.httpUpstreams?.length ?? 0,
  servers: current.value?.content.httpServers?.length ?? 0,
  ipPolicies: current.value?.content.ipPolicies?.length ?? 0,
  apiPolicies: current.value?.content.apiPolicies?.length ?? 0,
}));
const reportMetrics = computed(() => [{ label: '当前版本', value: current.value ? `v${current.value.versionNo}` : '未发布', hint: current.value ? formatTime(current.value.createdAt) : '等待首次发布', tone: 'blue' }, { label: '历史版本', value: versions.value.length, suffix: '个', hint: `${versions.value.filter((item) => item.state === 'ROLLED_BACK').length} 个回滚版本`, tone: 'purple' }, { label: '审计事件', value: auditEvents.value.length, suffix: '条', hint: `${auditEvents.value.filter((item) => item.result !== 'SUCCESS').length} 条失败`, tone: 'cyan' }, { label: '重载任务', value: reloadTasks.value.length, suffix: '次', hint: reloadTasks.value[0] ? reloadStateLabel(reloadTasks.value[0].status) : '暂无执行记录', tone: reloadTasks.value[0]?.status === 'SUCCESS' ? 'green' : 'orange' }]);
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

function formatJson(value: unknown) {
  return value === undefined ? '—' : JSON.stringify(value, null, 2);
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
    const requested = String(route.query.centerId || '');
    const target = centers.value.find((item) => item.id === requested) || centers.value[0];
    if (target) await selectCenter(target.id);
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
    versionPager.reset(); auditPager.reset(); reloadPager.reset();
    const [loadedVersions, loadedAudits, loadedCurrent, loadedReloads, loadedDraft, vp, ap, rp] = await Promise.all([
      request<RuntimeVersion[]>(`/centers/${centerId}/runtime-configurations`),
      request<AuditEvent[]>(`/centers/${centerId}/audit-events`),
      request<PublishedConfiguration>(`/centers/${centerId}/runtime-configurations/current`).catch(() => undefined),
      request<ReloadTask[]>(`/centers/${centerId}/control-api-reloads`).catch(() => []),
      request<DraftStatus>(`/centers/${centerId}/runtime-configurations/draft`),
      request<PageResult<RuntimeVersion>>(`/centers/${centerId}/runtime-configurations/paged?${versionPager.query()}`),
      request<PageResult<AuditEvent>>(`/centers/${centerId}/audit-events/paged?${auditPager.query()}`),
      request<PageResult<ReloadTask>>(`/centers/${centerId}/control-api-reloads/paged?${reloadPager.query()}`).catch(() => ({items:[],page:0,size:10,total:0,totalPages:0})),
    ]);
    versions.value = loadedVersions;
    auditEvents.value = loadedAudits;
    current.value = loadedCurrent;
    reloadTasks.value = loadedReloads;
    draft.value = loadedDraft;
    versionRows.value=versionPager.apply(vp); auditRows.value=auditPager.apply(ap); reloadRows.value=reloadPager.apply(rp);
    if (initialComparisonRequested.value) {
      initialComparisonRequested.value = false;
      await openComparison();
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载版本信息失败');
  } finally {
    loading.value = false;
  }
}

async function openComparison() {
  if (!selectedCenterId.value) return;
  comparing.value = true;
  try {
    comparison.value = await request<DraftComparison>(`/centers/${selectedCenterId.value}/runtime-configurations/draft/compare`);
    comparisonOpen.value = comparison.value.changedSections.length > 0;
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载配置差异失败');
  } finally {
    comparing.value = false;
  }
}

async function changePage(kind:'version'|'audit'|'reload', value:any){
  if(!selectedCenterId.value)return;
  const pager=kind==='version'?versionPager:kind==='audit'?auditPager:reloadPager; pager.change(value);
  const path=kind==='version'?'runtime-configurations/paged':kind==='audit'?'audit-events/paged':'control-api-reloads/paged';
  const result=await request<PageResult<any>>(`/centers/${selectedCenterId.value}/${path}?${pager.query()}`);
  const items=pager.apply(result); if(kind==='version')versionRows.value=items;else if(kind==='audit')auditRows.value=items;else reloadRows.value=items;
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

async function publish(): Promise<boolean> {
  if (!selectedCenterId.value) return false;
  publishing.value = true;
  try {
    const published = await request<PublishedConfiguration>(`/centers/${selectedCenterId.value}/runtime-configurations`, { method: 'POST' });
    message.success(published.changed ? `配置版本 v${published.versionNo} 已发布` : `配置未变化，仍使用 v${published.versionNo}`);
    window.dispatchEvent(new Event('openresty-config-published'));
    await selectCenter(selectedCenterId.value);
    return true;
  } catch (error) {
    message.error(error instanceof Error ? error.message : '发布运行配置失败');
    return false;
  } finally {
    publishing.value = false;
  }
}

async function publishFromComparison() {
  if (await publish()) comparisonOpen.value = false;
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
    window.dispatchEvent(new Event('openresty-config-published'));
    await selectCenter(selectedCenterId.value);
  } catch (error) {
    message.error(error instanceof Error ? error.message : '回滚运行配置失败');
  } finally {
    publishing.value = false;
  }
}

onMounted(() => {
  initialComparisonRequested.value = String(route.query.compare || '') === '1';
  loadCenters();
});
</script>

<template>
  <div class="ops-page">
    <a-card :bordered="false" title="运行配置版本与操作审计">
      <template #extra><section-help text="发布会将当前中心的 HTTP 配置和策略固化为 MySQL 不可变快照。Lua 工作进程按版本读取快照；监听端口等原生配置由 Control API 触发 reload。"/></template>
      <div class="flex flex-wrap items-center gap-3">
        <span>配置中心</span>
        <a-select v-model:value="selectedCenterId" class="w-80" placeholder="请选择中心" :options="centerOptions" @change="selectCenter" />
        <a-button :disabled="!selectedCenterId" :loading="comparing" type="primary" @click="openComparison">查看差异并发布</a-button>
        <a-button :disabled="!selectedCenterId" :loading="reloading" @click="reloadNativeConfiguration">生成原生配置并重载</a-button>
      </div>
    </a-card>

    <a-modal
      v-model:open="comparisonOpen"
      :confirm-loading="publishing"
      :mask-closable="!publishing"
      :ok-button-props="{ disabled: !comparisonRows.length }"
      :width="1240"
      cancel-text="暂不发布"
      ok-text="确认发布"
      title="发布前配置比对"
      @ok="publishFromComparison"
    >
      <a-alert
        show-icon
        :type="comparisonRows.length ? 'warning' : 'success'"
        :message="comparisonRows.length ? `检测到 ${comparisonRows.length} 类待发布变更` : '当前草稿与已发布版本一致'"
        :description="comparison?.publishedVersionNo ? `左侧为已发布 v${comparison.publishedVersionNo}，右侧为准备提交的当前草稿。` : '当前中心尚未发布过配置，右侧草稿会成为首个运行版本。'"
      />
      <a-empty v-if="!comparisonRows.length" class="py-12" description="没有需要发布的配置变更" />
      <div v-else class="comparison-list">
        <section v-for="item in comparisonRows" :key="item.key" class="comparison-section">
          <div class="comparison-heading">{{ item.label }} <span class="diff-count">{{ diffFor(item).length }} 处变化</span></div>
          <div class="git-diff">
            <div class="git-diff-header"><span>已发布 {{ comparison?.publishedVersionNo ? `v${comparison.publishedVersionNo}` : '（无）' }}</span><span>待发布草稿</span></div>
            <div v-for="diff in diffFor(item)" :key="`${item.key}-${diff.path}`" class="git-diff-row" :class="`diff-${diff.kind}`">
              <div class="diff-cell diff-path"><span class="diff-marker">{{ diff.kind === 'added' ? '+' : diff.kind === 'removed' ? '−' : '±' }}</span>{{ diff.path }}</div>
              <div class="diff-cell"><code>{{ diff.before }}</code></div>
              <div class="diff-cell"><code>{{ diff.after }}</code></div>
            </div>
          </div>
        </section>
      </div>
    </a-modal>

    <metric-grid :metrics="reportMetrics" />

    <a-row :gutter="[24,24]">
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
          <a-table :columns="versionColumns" :data-source="versionRows" :loading="loading" :pagination="versionPager.table.value" @change="(p:any)=>changePage('version',p)" row-key="id" size="small">
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
      <a-table :columns="auditColumns" :data-source="auditRows" :loading="loading" :pagination="auditPager.table.value" @change="(p:any)=>changePage('audit',p)" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</template>
          <template v-else-if="column.key === 'action'">{{ auditActionLabel(record.action) }}</template>
          <template v-else-if="column.key === 'result'"><a-tag :color="record.result === 'SUCCESS' ? 'green' : 'red'">{{ record.result === 'SUCCESS' ? '成功' : '失败' }}</a-tag></template>
        </template>
      </a-table>
    </a-card>

    <a-card class="mt-5" :bordered="false" :title="selectedCenter ? `${selectedCenter.name} 的原生配置重载记录` : '原生配置重载记录'">
      <a-table :columns="reloadColumns" :data-source="reloadRows" :loading="loading" :pagination="reloadPager.table.value" @change="(p:any)=>changePage('reload',p)" row-key="id">
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
.comparison-list { max-height: calc(100vh - 310px); margin-top: 16px; overflow: auto; }
.comparison-section { border: 1px solid var(--ant-color-border-secondary); border-radius: 8px; padding: 16px; }
.comparison-section + .comparison-section { margin-top: 12px; }
.comparison-heading { margin-bottom: 12px; font-weight: 600; }
.diff-count { margin-left: 8px; color: var(--ant-color-text-tertiary); font-size: 12px; font-weight: 400; }
.git-diff { overflow: hidden; border: 1px solid var(--ant-color-border-secondary); border-radius: 6px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 12px; }
.git-diff-header, .git-diff-row { display: grid; grid-template-columns: minmax(220px, .9fr) minmax(0, 1fr) minmax(0, 1fr); }
.git-diff-header { padding: 8px 12px; color: var(--ant-color-text-secondary); background: var(--ant-color-fill-quaternary); font-family: inherit; font-weight: 600; }
.git-diff-header span:nth-child(2) { grid-column: 3; }
.git-diff-row { border-top: 1px solid var(--ant-color-border-secondary); }
.diff-cell { min-width: 0; padding: 7px 10px; overflow-wrap: anywhere; white-space: pre-wrap; }
.diff-path { border-right: 1px solid var(--ant-color-border-secondary); color: var(--ant-color-text-secondary); }
.diff-cell + .diff-cell { border-left: 1px solid var(--ant-color-border-secondary); }
.diff-cell code { color: inherit; font-family: inherit; white-space: pre-wrap; }
.diff-marker { display: inline-block; width: 18px; color: var(--ant-color-text-tertiary); font-weight: 700; }
.diff-added .diff-cell:nth-child(3) { background: rgb(82 196 26 / 12%); color: #b7eb8f; }
.diff-removed .diff-cell:nth-child(2) { background: rgb(255 77 79 / 12%); color: #ffccc7; }
.diff-changed .diff-cell:nth-child(2) { background: rgb(255 77 79 / 10%); color: #ffccc7; }
.diff-changed .diff-cell:nth-child(3) { background: rgb(82 196 26 / 10%); color: #b7eb8f; }
.comparison-pane { min-height: 140px; overflow: auto; border-radius: 6px; padding: 12px; }
.comparison-before { border: 1px solid rgb(255 77 79 / 38%); background: rgb(255 77 79 / 5%); }
.comparison-after { border: 1px solid rgb(82 196 26 / 38%); background: rgb(82 196 26 / 5%); }
.comparison-label { display: block; margin-bottom: 8px; font-size: 12px; font-weight: 600; }
.comparison-pane pre { margin: 0; overflow-wrap: anywhere; white-space: pre-wrap; font-size: 12px; line-height: 1.55; }
</style>
