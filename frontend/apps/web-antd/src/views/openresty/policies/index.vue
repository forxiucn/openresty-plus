<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';

import { message } from 'ant-design-vue';
import MetricGrid from '#/components/operations/MetricGrid.vue';

type Center = { id: string; code: string; name: string };
type HttpServer = { id: string; domain: string; listenPort: number };
type HttpLocation = { id: string; path: string };
type StreamServer = { id: string; serviceName: string; listenPort: number; protocol: 'TCP' | 'UDP' };
type IpScope = 'STREAM' | 'HTTP_SERVER' | 'HTTP_LOCATION';
type Mode = 'BLACKLIST' | 'WHITELIST';
type IpPolicy = { id: string; mode: Mode; priority: number; scope: IpScope; targetResourceId: string; enabled: boolean; ipRules: string[] };
type ApiRule = { method: string; path: string };
type ApiPolicy = { id: string; mode: Mode; priority: number; httpLocationId: string; enabled: boolean; rules: ApiRule[] };

const centers = ref<Center[]>([]);
const servers = ref<HttpServer[]>([]);
const locations = ref<(HttpLocation & { serverId: string; label: string })[]>([]);
const streamServers = ref<StreamServer[]>([]);
const ipPolicies = ref<IpPolicy[]>([]);
const apiPolicies = ref<ApiPolicy[]>([]);
const selectedCenterId = ref<string>();
const loading = ref(false);
const ipDrawerOpen = ref(false);
const apiDrawerOpen = ref(false);
const editingIpId = ref<string>();
const editingApiId = ref<string>();

const newIpForm = () => ({ mode: 'BLACKLIST' as Mode, priority: 100, scope: 'HTTP_SERVER' as IpScope, targetResourceId: undefined as string | undefined, enabled: true, ipRules: [] as string[] });
const newApiForm = () => ({ mode: 'BLACKLIST' as Mode, priority: 100, httpLocationId: undefined as string | undefined, enabled: true, rules: [{ method: 'GET', path: '/' }] as ApiRule[] });
const ipForm = ref(newIpForm());
const apiForm = ref(newApiForm());

const centerOptions = computed(() => centers.value.map((value) => ({ value: value.id, label: `${value.name}（${value.code}）` })));
const modeOptions = [
  { value: 'BLACKLIST', label: '黑名单：命中后拒绝（BLACKLIST）' },
  { value: 'WHITELIST', label: '白名单：仅允许命中项（WHITELIST）' },
];
const scopeOptions = [
  { value: 'HTTP_SERVER', label: '七层：域名与端口（HTTP_SERVER）' },
  { value: 'HTTP_LOCATION', label: '七层：指定接口（HTTP_LOCATION）' },
  { value: 'STREAM', label: '四层：服务端口（STREAM）' },
];
const methodNames: Record<string, string> = { GET: '读取', POST: '创建', PUT: '全量更新', PATCH: '部分更新', DELETE: '删除', HEAD: '响应头', OPTIONS: '预检' };
const methodOptions = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].map((value) => ({ value, label: `${methodNames[value]}（${value}）` }));
const scopeLabel = (value: IpScope) => ({ STREAM: '四层：服务端口', HTTP_SERVER: '七层：域名与端口', HTTP_LOCATION: '七层：指定接口' })[value];
const modeLabel = (value: Mode) => value === 'BLACKLIST' ? '黑名单' : '白名单';
const targetOptions = computed(() => {
  if (ipForm.value.scope === 'HTTP_SERVER') return servers.value.map((value) => ({ value: value.id, label: `${value.domain}:${value.listenPort}` }));
  if (ipForm.value.scope === 'HTTP_LOCATION') return locations.value.map((value) => ({ value: value.id, label: value.label }));
  return streamServers.value.map((value) => ({ value: value.id, label: `${value.serviceName}:${value.listenPort}（${value.protocol}）` }));
});
const locationOptions = computed(() => locations.value.map((value) => ({ value: value.id, label: value.label })));
const reportMetrics = computed(() => [{ label: 'IP 策略', value: ipPolicies.value.length, suffix: '条', hint: `${ipPolicies.value.filter((item) => item.enabled).length} 条已启用`, tone: 'blue' }, { label: 'API 策略', value: apiPolicies.value.length, suffix: '条', hint: `${apiPolicies.value.reduce((sum, item) => sum + item.rules.length, 0)} 条匹配规则`, tone: 'purple' }, { label: '黑名单', value: [...ipPolicies.value, ...apiPolicies.value].filter((item) => item.mode === 'BLACKLIST').length, suffix: '条', hint: '命中后拒绝', tone: 'red' }, { label: '白名单', value: [...ipPolicies.value, ...apiPolicies.value].filter((item) => item.mode === 'WHITELIST').length, suffix: '条', hint: '仅允许命中项', tone: 'green' }]);
const locationLabel = (id: string) => locations.value.find((value) => value.id === id)?.label || '配置已删除';
const targetLabel = (policy: IpPolicy) => {
  if (policy.scope === 'HTTP_SERVER') return servers.value.find((value) => value.id === policy.targetResourceId) ? `${servers.value.find((value) => value.id === policy.targetResourceId)?.domain}:${servers.value.find((value) => value.id === policy.targetResourceId)?.listenPort}` : '配置已删除';
  if (policy.scope === 'HTTP_LOCATION') return locationLabel(policy.targetResourceId);
  const target = streamServers.value.find((value) => value.id === policy.targetResourceId);
  return target ? `${target.serviceName}:${target.listenPort}（${target.protocol}）` : '配置已删除';
};

const ipColumns = [
  { dataIndex: 'scope', key: 'scope', title: '生效范围', width: 150 },
  { key: 'target', title: '目标' },
  { dataIndex: 'mode', key: 'mode', title: '模式', width: 100 },
  { dataIndex: 'priority', key: 'priority', title: '优先级', width: 80 },
  { dataIndex: 'ipRules', key: 'ipRules', title: 'IP / 网段' },
  { dataIndex: 'enabled', key: 'enabled', title: '状态', width: 90 },
  { key: 'action', title: '操作', width: 130 },
];
const apiColumns = [
  { dataIndex: 'httpLocationId', key: 'location', title: '生效接口' },
  { dataIndex: 'mode', key: 'mode', title: '模式', width: 100 },
  { dataIndex: 'priority', key: 'priority', title: '优先级', width: 80 },
  { dataIndex: 'rules', key: 'rules', title: '匹配规则' },
  { dataIndex: 'enabled', key: 'enabled', title: '状态', width: 90 },
  { key: 'action', title: '操作', width: 130 },
];

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) } });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail || body.message || '请求失败');
  }
  return response.status === 204 ? (undefined as T) : response.json() as Promise<T>;
}

async function loadCenters() {
  loading.value = true;
  try {
    centers.value = await request<Center[]>('/centers');
    if (!selectedCenterId.value && centers.value[0]) await selectCenter(centers.value[0].id);
  } catch (error) { message.error(error instanceof Error ? error.message : '加载中心失败'); }
  finally { loading.value = false; }
}

async function selectCenter(centerId: string) {
  selectedCenterId.value = centerId;
  loading.value = true;
  try {
    const [loadedServers, loadedStreamServers, loadedIpPolicies, loadedApiPolicies] = await Promise.all([
      request<HttpServer[]>(`/centers/${centerId}/http/servers`),
      request<StreamServer[]>(`/centers/${centerId}/stream/servers`),
      request<IpPolicy[]>(`/centers/${centerId}/ip-policies`),
      request<ApiPolicy[]>(`/centers/${centerId}/api-policies`),
    ]);
    servers.value = loadedServers;
    streamServers.value = loadedStreamServers;
    ipPolicies.value = loadedIpPolicies;
    apiPolicies.value = loadedApiPolicies;
    const groups = await Promise.all(loadedServers.map(async (server) => {
      const values = await request<HttpLocation[]>(`/centers/${centerId}/http/servers/${server.id}/locations`);
      return values.map((location) => ({ ...location, serverId: server.id, label: `${server.domain}:${server.listenPort}${location.path}` }));
    }));
    locations.value = groups.flat();
  } catch (error) { message.error(error instanceof Error ? error.message : '加载策略失败'); }
  finally { loading.value = false; }
}

function openIp(policy?: IpPolicy) {
  editingIpId.value = policy?.id;
  ipForm.value = policy ? { mode: policy.mode, priority: policy.priority, scope: policy.scope, targetResourceId: policy.targetResourceId, enabled: policy.enabled, ipRules: [...policy.ipRules] } : newIpForm();
  ipDrawerOpen.value = true;
}
function openApi(policy?: ApiPolicy) {
  editingApiId.value = policy?.id;
  apiForm.value = policy ? { mode: policy.mode, priority: policy.priority, httpLocationId: policy.httpLocationId, enabled: policy.enabled, rules: policy.rules.map((rule) => ({ ...rule })) } : newApiForm();
  apiDrawerOpen.value = true;
}
function changeScope() { ipForm.value.targetResourceId = undefined; }
function addRule() { apiForm.value.rules.push({ method: 'GET', path: '/' }); }
function removeRule(index: number) { if (apiForm.value.rules.length > 1) apiForm.value.rules.splice(index, 1); }

async function saveIp() {
  if (!selectedCenterId.value || !ipForm.value.targetResourceId || !ipForm.value.ipRules.length) {
    message.warning('请填写生效范围、目标和至少一条 IP 或网段'); return;
  }
  try {
    const url = `/centers/${selectedCenterId.value}/ip-policies${editingIpId.value ? `/${editingIpId.value}` : ''}`;
    await request(url, { method: editingIpId.value ? 'PUT' : 'POST', body: JSON.stringify(ipForm.value) });
    ipDrawerOpen.value = false;
    await selectCenter(selectedCenterId.value);
    message.success(editingIpId.value ? 'IP 策略已更新' : 'IP 策略已创建');
  } catch (error) { message.error(error instanceof Error ? error.message : '保存 IP 策略失败'); }
}
async function saveApi() {
  if (!selectedCenterId.value || !apiForm.value.httpLocationId || !apiForm.value.rules.every((rule) => rule.method && rule.path)) {
    message.warning('请选择接口并完整填写至少一条匹配规则'); return;
  }
  try {
    const url = `/centers/${selectedCenterId.value}/api-policies${editingApiId.value ? `/${editingApiId.value}` : ''}`;
    await request(url, { method: editingApiId.value ? 'PUT' : 'POST', body: JSON.stringify(apiForm.value) });
    apiDrawerOpen.value = false;
    await selectCenter(selectedCenterId.value);
    message.success(editingApiId.value ? '接口策略已更新' : '接口策略已创建');
  } catch (error) { message.error(error instanceof Error ? error.message : '保存接口策略失败'); }
}
async function removePolicy(type: 'api' | 'ip', id: string) {
  if (!selectedCenterId.value) return;
  try {
    await request(`/centers/${selectedCenterId.value}/${type}-policies/${id}`, { method: 'DELETE' });
    await selectCenter(selectedCenterId.value);
    message.success(type === 'ip' ? 'IP 策略已删除' : '接口策略已删除');
  } catch (error) { message.error(error instanceof Error ? error.message : '删除策略失败'); }
}
async function movePolicy(type: 'api' | 'ip', id: string, direction: 'UP' | 'DOWN') {
  if (!selectedCenterId.value) return;
  try {
    const result = await request<IpPolicy[] | ApiPolicy[]>(`/centers/${selectedCenterId.value}/${type}-policies/${id}/move?direction=${direction}`, { method: 'POST' });
    if (type === 'ip') ipPolicies.value = result as IpPolicy[];
    else apiPolicies.value = result as ApiPolicy[];
  } catch (error) { message.error(error instanceof Error ? error.message : '调整策略顺序失败'); }
}

onMounted(loadCenters);
</script>

<template>
  <div class="ops-page">
    <a-card :bordered="false" title="访问策略管理">
      <a-alert class="mb-4" show-icon type="info" message="策略保存后需在“版本与审计”中发布，运行中的 OpenResty 节点会读取最新快照。数值越小，优先级越高。" />
      <a-form layout="inline">
        <a-form-item label="配置中心"><a-select v-model:value="selectedCenterId" class="w-72" placeholder="请选择中心" :options="centerOptions" @change="selectCenter" /></a-form-item>
      </a-form>
    </a-card>

    <metric-grid :metrics="reportMetrics" />

    <a-row :gutter="[24,24]">
      <a-col :lg="12" :xs="24">
        <a-card :bordered="false" :loading="loading" title="IP 访问策略">
          <template #extra><a-button :disabled="!selectedCenterId" type="primary" @click="openIp()">新增 IP 策略</a-button></template>
          <a-table :columns="ipColumns" :data-source="ipPolicies" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'scope'">{{ scopeLabel(record.scope) }}</template>
              <template v-else-if="column.key === 'target'">{{ targetLabel(record) }}</template>
              <template v-else-if="column.key === 'mode'"><a-tag :color="record.mode === 'BLACKLIST' ? 'red' : 'green'">{{ modeLabel(record.mode) }}</a-tag></template>
              <template v-else-if="column.key === 'ipRules'"><a-tag v-for="item in record.ipRules" :key="item">{{ item }}</a-tag></template>
              <template v-else-if="column.key === 'enabled'"><a-tag :color="record.enabled ? 'green' : 'default'">{{ record.enabled ? '已启用' : '未启用' }}</a-tag></template>
              <template v-else-if="column.key === 'action'"><a-space><a-button type="link" @click="movePolicy('ip', record.id, 'UP')">上移</a-button><a-button type="link" @click="movePolicy('ip', record.id, 'DOWN')">下移</a-button><a-button type="link" @click="openIp(record)">编辑</a-button><a-popconfirm title="确认删除该 IP 策略？" @confirm="removePolicy('ip', record.id)"><a-button danger type="link">删除</a-button></a-popconfirm></a-space></template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :lg="12" :xs="24" class="max-lg:mt-4">
        <a-card :bordered="false" :loading="loading" title="接口访问策略">
          <template #extra><a-button :disabled="!selectedCenterId || !locations.length" type="primary" @click="openApi()">新增接口策略</a-button></template>
          <a-table :columns="apiColumns" :data-source="apiPolicies" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'location'">{{ locationLabel(record.httpLocationId) }}</template>
              <template v-else-if="column.key === 'mode'"><a-tag :color="record.mode === 'BLACKLIST' ? 'red' : 'green'">{{ modeLabel(record.mode) }}</a-tag></template>
              <template v-else-if="column.key === 'rules'"><a-tag v-for="rule in record.rules" :key="`${rule.method}-${rule.path}`">{{ rule.method }} {{ rule.path }}</a-tag></template>
              <template v-else-if="column.key === 'enabled'"><a-tag :color="record.enabled ? 'green' : 'default'">{{ record.enabled ? '已启用' : '未启用' }}</a-tag></template>
              <template v-else-if="column.key === 'action'"><a-space><a-button type="link" @click="movePolicy('api', record.id, 'UP')">上移</a-button><a-button type="link" @click="movePolicy('api', record.id, 'DOWN')">下移</a-button><a-button type="link" @click="openApi(record)">编辑</a-button><a-popconfirm title="确认删除该接口策略？" @confirm="removePolicy('api', record.id)"><a-button danger type="link">删除</a-button></a-popconfirm></a-space></template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-drawer v-model:open="ipDrawerOpen" :title="editingIpId ? '编辑 IP 策略' : '新增 IP 策略'" :width="560">
      <a-form layout="vertical">
        <a-form-item label="策略模式" required><a-select v-model:value="ipForm.mode" :options="modeOptions" /></a-form-item>
        <a-form-item label="生效范围" required><a-select v-model:value="ipForm.scope" :options="scopeOptions" @change="changeScope" /></a-form-item>
        <a-form-item label="生效目标" required><a-select v-model:value="ipForm.targetResourceId" :options="targetOptions" placeholder="请选择配置目标" /></a-form-item>
        <a-form-item label="优先级" extra="多个策略同时命中时，数值越小越优先。"><a-input-number v-model:value="ipForm.priority" class="w-full" :min="0" /></a-form-item>
        <a-form-item label="IP 地址或 CIDR 网段" required extra="可输入 IPv4、IPv6 或 CIDR 网段，例如 10.0.0.0/24。"><a-select v-model:value="ipForm.ipRules" mode="tags" placeholder="输入后按回车添加，可添加多项" /></a-form-item>
        <a-form-item label="启用状态"><a-switch v-model:checked="ipForm.enabled" checked-children="启用" un-checked-children="停用" /></a-form-item>
      </a-form>
      <template #footer><div class="flex justify-end gap-2"><a-button @click="ipDrawerOpen = false">取消</a-button><a-button type="primary" @click="saveIp">保存</a-button></div></template>
    </a-drawer>

    <a-drawer v-model:open="apiDrawerOpen" :title="editingApiId ? '编辑接口策略' : '新增接口策略'" :width="680">
      <a-form layout="vertical">
        <a-row :gutter="16"><a-col :span="12"><a-form-item label="策略模式" required><a-select v-model:value="apiForm.mode" :options="modeOptions" /></a-form-item></a-col><a-col :span="12"><a-form-item label="优先级" extra="数值越小越优先。"><a-input-number v-model:value="apiForm.priority" class="w-full" :min="0" /></a-form-item></a-col></a-row>
        <a-form-item label="生效接口" required><a-select v-model:value="apiForm.httpLocationId" :options="locationOptions" placeholder="请选择 HTTP Location" /></a-form-item>
        <a-form-item label="启用状态"><a-switch v-model:checked="apiForm.enabled" checked-children="启用" un-checked-children="停用" /></a-form-item>
        <a-divider orientation="left">匹配规则</a-divider>
        <div v-for="(rule, index) in apiForm.rules" :key="index" class="mb-3 flex gap-2">
          <a-select v-model:value="rule.method" class="w-32" :options="methodOptions" />
          <a-input v-model:value="rule.path" class="flex-1" placeholder="例如 /orders/* 或 /health" />
          <a-button :disabled="apiForm.rules.length === 1" danger @click="removeRule(index)">移除</a-button>
        </div>
        <a-button type="dashed" block @click="addRule">添加匹配规则</a-button>
      </a-form>
      <template #footer><div class="flex justify-end gap-2"><a-button @click="apiDrawerOpen = false">取消</a-button><a-button type="primary" @click="saveApi">保存</a-button></div></template>
    </a-drawer>
  </div>
</template>
