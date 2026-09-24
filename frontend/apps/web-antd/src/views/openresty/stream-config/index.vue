<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import { Alert as AAlert, Button as AButton, Card as ACard, Checkbox as ACheckbox, Col as ACol, Drawer as ADrawer, Empty as AEmpty, Form as AForm, FormItem as AFormItem, Input as AInput, InputNumber as AInputNumber, Row as ARow, Select as ASelect, Table as ATable, Tag as ATag, message } from 'ant-design-vue';
import MetricGrid from '#/components/operations/MetricGrid.vue';
import { type PageResult, useServerPagination } from '#/utils/server-pagination';

type Center = { id: string; code: string; name: string };
type StreamUpstream = { id: string; name: string; targetHost: string; targetPort: number };
type StreamServer = { id: string; serviceName: string; listenPort: number; protocol: 'TCP' | 'UDP'; upstreamId: string; accessLog: string; errorLog: string; dynamicDnsEnabled: boolean; dynamicDnsHost?: string; dynamicDnsPort?: number };

const centers = ref<Center[]>([]);
const upstreams = ref<StreamUpstream[]>([]);
const servers = ref<StreamServer[]>([]);
const upstreamRows=ref<StreamUpstream[]>([]),serverRows=ref<StreamServer[]>([]);
const upstreamPager=useServerPagination(),serverPager=useServerPagination();
const selectedCenterId = ref<string>();
const loading = ref(false);
const upstreamOpen = ref(false);
const serverOpen = ref(false);
const editingUpstreamId = ref<string>();
const editingServerId = ref<string>();
const freshUpstream = () => ({ name: '', targetHost: '', targetPort: 3306 });
const freshServer = () => ({ serviceName: '', listenPort: 3306, protocol: 'TCP' as 'TCP' | 'UDP', upstreamId: undefined as string | undefined, accessLog: '', errorLog: '', dynamicDnsEnabled: false, dynamicDnsHost: '', dynamicDnsPort: 3306 });
const upstreamForm = ref(freshUpstream());
const serverForm = ref(freshServer());

const selectedCenter = computed(() => centers.value.find((item) => item.id === selectedCenterId.value));
const centerOptions = computed(() => centers.value.map((item) => ({ value: item.id, label: `${item.name}（${item.code}）` })));
const upstreamOptions = computed(() => upstreams.value.map((item) => ({ value: item.id, label: `${item.name} → ${item.targetHost}:${item.targetPort}` })));
const upstreamName = (id: string) => upstreams.value.find((item) => item.id === id)?.name ?? '已删除的 Upstream';
const protocolLabel = (protocol: 'TCP' | 'UDP') => protocol === 'TCP' ? 'TCP（传输控制协议）' : 'UDP（用户数据报协议）';
const upstreamColumns = [{ dataIndex: 'name', key: 'name', title: '服务标识' }, { dataIndex: 'targetHost', key: 'targetHost', title: '目标地址' }, { dataIndex: 'targetPort', key: 'targetPort', title: '目标端口', width: 105 }, { key: 'action', title: '操作', width: 132 }];
const serverColumns = [{ dataIndex: 'serviceName', key: 'serviceName', title: '服务名称' }, { dataIndex: 'listenPort', key: 'listenPort', title: '监听端口', width: 105 }, { dataIndex: 'protocol', key: 'protocol', title: '协议', width: 160 }, { dataIndex: 'upstreamId', key: 'upstreamId', title: '转发目标' }, { key: 'action', title: '操作', width: 132 }];
const reportMetrics = computed(() => [{ label: '监听服务', value: servers.value.length, suffix: '个', hint: `${new Set(servers.value.map((item) => item.listenPort)).size} 个端口`, tone: 'blue' }, { label: 'TCP 服务', value: servers.value.filter((item) => item.protocol === 'TCP').length, suffix: '个', hint: '面向连接代理', tone: 'cyan' }, { label: 'UDP 服务', value: servers.value.filter((item) => item.protocol === 'UDP').length, suffix: '个', hint: '无连接代理', tone: 'purple' }, { label: 'Stream Upstream', value: upstreams.value.length, suffix: '组', hint: `${servers.value.filter((item) => item.dynamicDnsEnabled).length} 个动态解析服务`, tone: 'green' }]);

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) } });
  if (!response.ok) { const body = await response.json().catch(() => ({})); throw new Error(body.detail || body.message || '请求失败'); }
  return response.status === 204 ? (undefined as T) : response.json() as Promise<T>;
}
async function loadStreamPage(kind:'upstream'|'server') { if(!selectedCenterId.value)return; const pager=kind==='upstream'?upstreamPager:serverPager; const path=kind==='upstream'?'upstreams/paged':'servers/paged'; const result=await request<PageResult<any>>(`/centers/${selectedCenterId.value}/stream/${path}?${pager.query()}`); const items=pager.apply(result); if(kind==='upstream')upstreamRows.value=items;else serverRows.value=items; }
async function changeStreamPage(kind:'upstream'|'server',p:any){const pager=kind==='upstream'?upstreamPager:serverPager;pager.change(p);await loadStreamPage(kind)}
async function loadCenter(centerId: string) {
  selectedCenterId.value = centerId; loading.value = true;
  upstreamPager.reset(); serverPager.reset();
  try { [upstreams.value, servers.value] = await Promise.all([request<StreamUpstream[]>(`/centers/${centerId}/stream/upstreams`), request<StreamServer[]>(`/centers/${centerId}/stream/servers`)]); await Promise.all([loadStreamPage('upstream'),loadStreamPage('server')]); }
  catch (error) { message.error(error instanceof Error ? error.message : '加载四层配置失败'); }
  finally { loading.value = false; }
}
async function load() {
  try { centers.value = await request<Center[]>('/centers'); if (centers.value[0]) await loadCenter(centers.value[0].id); }
  catch (error) { message.error(error instanceof Error ? error.message : '加载中心失败'); }
}
function openUpstream(value?: StreamUpstream) { editingUpstreamId.value = value?.id; upstreamForm.value = value ? { name: value.name, targetHost: value.targetHost, targetPort: value.targetPort } : freshUpstream(); upstreamOpen.value = true; }
function openServer(value?: StreamServer) { editingServerId.value = value?.id; serverForm.value = value ? { serviceName: value.serviceName, listenPort: value.listenPort, protocol: value.protocol, upstreamId: value.upstreamId, accessLog: value.accessLog || '', errorLog: value.errorLog || '', dynamicDnsEnabled: value.dynamicDnsEnabled, dynamicDnsHost: value.dynamicDnsHost || '', dynamicDnsPort: value.dynamicDnsPort || 3306 } : freshServer(); serverOpen.value = true; }
async function saveUpstream() {
  if (!selectedCenterId.value) return;
  if (!upstreamForm.value.name.trim() || !upstreamForm.value.targetHost.trim()) { message.warning('请填写服务标识和目标地址'); return; }
  try { await request(`/centers/${selectedCenterId.value}/stream/upstreams${editingUpstreamId.value ? `/${editingUpstreamId.value}` : ''}`, { method: editingUpstreamId.value ? 'PUT' : 'POST', body: JSON.stringify(upstreamForm.value) }); upstreamOpen.value = false; await loadCenter(selectedCenterId.value); message.success(editingUpstreamId.value ? '四层 Upstream 已更新' : '四层 Upstream 已创建'); }
  catch (error) { message.error(error instanceof Error ? error.message : '保存失败'); }
}
async function saveServer() {
  if (!selectedCenterId.value) return;
  if (!serverForm.value.serviceName.trim() || !serverForm.value.upstreamId) { message.warning('请填写服务名称并选择转发 Upstream'); return; }
  try { await request(`/centers/${selectedCenterId.value}/stream/servers${editingServerId.value ? `/${editingServerId.value}` : ''}`, { method: editingServerId.value ? 'PUT' : 'POST', body: JSON.stringify(serverForm.value) }); serverOpen.value = false; await loadCenter(selectedCenterId.value); message.success(editingServerId.value ? '四层 Server 已更新' : '四层 Server 已创建'); }
  catch (error) { message.error(error instanceof Error ? error.message : '保存失败'); }
}
async function remove(kind: 'servers' | 'upstreams', id: string, title: string) {
  if (!selectedCenterId.value) return;
  try { await request(`/centers/${selectedCenterId.value}/stream/${kind}/${id}`, { method: 'DELETE' }); await loadCenter(selectedCenterId.value); message.success(`${title}已删除`); }
  catch (error) { message.error(error instanceof Error ? error.message : '删除失败'); }
}
onMounted(load);
</script>

<template>
  <div class="ops-page">
    <a-card :bordered="false" title="四层 Stream 配置">
      <a-alert class="mb-4" show-icon type="info" message="先配置转发目标，再创建监听服务" description="Upstream 定义 TCP/UDP 后端地址；Server 定义协议和监听端口。保存后发布运行时快照；新增、修改或删除监听端口后，请在“版本与审计”执行原生配置重载。" />
      <a-form layout="inline">
        <a-form-item label="配置中心"><a-select v-model:value="selectedCenterId" class="w-80" :loading="loading && !selectedCenter" :options="centerOptions" placeholder="请选择配置中心" @change="loadCenter" /></a-form-item>
        <a-form-item v-if="selectedCenter"><span class="text-gray-500">当前中心：{{ selectedCenter.name }}，{{ upstreams.length }} 个转发目标，{{ servers.length }} 个监听服务</span></a-form-item>
      </a-form>
    </a-card>
    <metric-grid :metrics="reportMetrics" />
    <a-row :gutter="[24,24]">
      <a-col :lg="11" :xs="24">
        <a-card :bordered="false" :title="selectedCenter ? `${selectedCenter.name} 的转发目标` : '转发目标'">
          <template #extra><a-button type="primary" :disabled="!selectedCenterId" @click="openUpstream()">新增 Upstream</a-button></template>
          <a-table :columns="upstreamColumns" :data-source="upstreamRows" :loading="loading" :pagination="upstreamPager.table.value" @change="(p:any)=>changeStreamPage('upstream',p)" row-key="id">
            <template #emptyText><a-empty description="还没有转发目标，请先新增 Upstream" /></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'targetHost'"><span class="font-mono">{{ record.targetHost }}</span></template>
              <template v-else-if="column.key === 'action'"><a-button type="link" @click="openUpstream(record)">编辑</a-button><a-popconfirm title="确认删除该 Upstream？" description="已关联的监听服务需要先调整或删除。" ok-text="删除" cancel-text="取消" @confirm="remove('upstreams', record.id, '四层 Upstream')"><a-button danger type="link">删除</a-button></a-popconfirm></template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :lg="13" :xs="24" class="max-lg:mt-4">
        <a-card :bordered="false" :title="selectedCenter ? `${selectedCenter.name} 的监听服务` : '监听服务'">
          <template #extra><a-button type="primary" :disabled="!selectedCenterId || !upstreams.length" @click="openServer()">新增 Server</a-button></template>
          <a-alert v-if="selectedCenterId && !upstreams.length && !loading" class="mb-3" type="warning" show-icon message="请先创建至少一个 Upstream，才能创建监听服务。" />
          <a-table :columns="serverColumns" :data-source="serverRows" :loading="loading" :pagination="serverPager.table.value" @change="(p:any)=>changeStreamPage('server',p)" row-key="id">
            <template #emptyText><a-empty description="还没有监听服务，请新增 Server" /></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'protocol'"><a-tag :color="record.protocol === 'TCP' ? 'blue' : 'purple'">{{ protocolLabel(record.protocol) }}</a-tag></template>
              <template v-else-if="column.key === 'upstreamId'">{{ upstreamName(record.upstreamId) }}</template>
              <template v-else-if="column.key === 'action'"><a-button type="link" @click="openServer(record)">编辑</a-button><a-popconfirm title="确认删除该监听服务？" description="删除后需发布并执行原生配置重载，端口才会停止监听。" ok-text="删除" cancel-text="取消" @confirm="remove('servers', record.id, '四层 Server')"><a-button danger type="link">删除</a-button></a-popconfirm></template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
    <a-drawer v-model:open="upstreamOpen" :title="editingUpstreamId ? '编辑四层 Upstream' : '新增四层 Upstream'" :width="480">
      <a-alert class="mb-4" type="info" show-icon message="每个 Upstream 指向一个后端地址和端口。" />
      <a-form layout="vertical"><a-form-item label="服务标识" required extra="用于在监听服务中选择此转发目标，例如 mysql-primary。"><a-input v-model:value="upstreamForm.name" placeholder="例如：mysql-primary" /></a-form-item><a-form-item label="目标地址" required extra="可填写 IP 地址或可由 OpenResty 解析的域名。"><a-input v-model:value="upstreamForm.targetHost" placeholder="例如：10.10.0.15" /></a-form-item><a-form-item label="目标端口" required><a-input-number v-model:value="upstreamForm.targetPort" class="w-full" :min="1" :max="65535" /></a-form-item></a-form>
      <template #footer><div class="flex justify-end gap-2"><a-button @click="upstreamOpen = false">取消</a-button><a-button type="primary" @click="saveUpstream">保存</a-button></div></template>
    </a-drawer>
    <a-drawer v-model:open="serverOpen" :title="editingServerId ? '编辑四层 Server' : '新增四层 Server'" :width="520">
      <a-alert class="mb-4" type="warning" show-icon message="监听端口变更需要发布，并在“版本与审计”执行原生配置重载后才会生效。" />
      <a-form layout="vertical"><a-form-item label="服务名称" required extra="用于识别该监听服务，例如 mysql-proxy。"><a-input v-model:value="serverForm.serviceName" placeholder="例如：mysql-proxy" /></a-form-item><a-form-item label="监听端口" required><a-input-number v-model:value="serverForm.listenPort" class="w-full" :min="1" :max="65535" /></a-form-item><a-form-item label="传输协议" required><a-select v-model:value="serverForm.protocol" :options="[{ value: 'TCP', label: '传输控制协议（TCP）' }, { value: 'UDP', label: '用户数据报协议（UDP）' }]" /></a-form-item><a-form-item label="转发 Upstream" required><a-select v-model:value="serverForm.upstreamId" :options="upstreamOptions" placeholder="请选择已配置的转发目标" /></a-form-item><a-form-item><a-checkbox v-model:checked="serverForm.dynamicDnsEnabled">使用变量动态解析目标域名</a-checkbox></a-form-item><a-row v-if="serverForm.dynamicDnsEnabled" :gutter="16"><a-col :span="16"><a-form-item label="目标域名" required><a-input v-model:value="serverForm.dynamicDnsHost" placeholder="mysql.internal.example.com" /></a-form-item></a-col><a-col :span="8"><a-form-item label="目标端口" required><a-input-number v-model:value="serverForm.dynamicDnsPort" class="w-full" :min="1" :max="65535" /></a-form-item></a-col></a-row><a-form-item label="访问日志" extra="留空时由系统按服务和端口生成。"><a-input v-model:value="serverForm.accessLog" placeholder="留空自动生成" /></a-form-item><a-form-item label="错误日志" extra="留空时由系统按服务和端口生成。"><a-input v-model:value="serverForm.errorLog" placeholder="留空自动生成" /></a-form-item></a-form>
      <template #footer><div class="flex justify-end gap-2"><a-button @click="serverOpen = false">取消</a-button><a-button type="primary" @click="saveServer">保存</a-button></div></template>
    </a-drawer>
  </div>
</template>
