<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import {
  Alert as AAlert,
  Button as AButton,
  Card as ACard,
  Checkbox as ACheckbox,
  Drawer as ADrawer,
  Empty as AEmpty,
  Form as AForm,
  FormItem as AFormItem,
  Input as AInput,
  InputNumber as AInputNumber,
  Popconfirm as APopconfirm,
  Select as ASelect,
  Table as ATable,
  Tabs as ATabs,
  TabPane as ATabPane,
  Tag as ATag,
  message,
} from 'ant-design-vue';

type Center = { id: string; code: string; name: string };
type HttpTarget = { id: string; targetHost: string; targetPort: number; weight: number; maxFails: number; failTimeoutSeconds: number; resolveEnabled: boolean; backup: boolean; enabled: boolean };
type HealthResult = { targetId?: string; targetHost: string; targetPort: number; status: string; httpStatus?: number; message: string };
type HttpUpstream = { id: string; name: string; keepaliveConnections: number; zoneSizeKilobytes: number; healthCheckEnabled: boolean; healthCheckPath: string; healthCheckIntervalSeconds: number; healthCheckTimeoutMilliseconds: number; healthCheckExpectedStatus: number; targets: HttpTarget[] };
type StreamUpstream = { id: string; name: string; targetHost: string; targetPort: number; resolveEnabled: boolean; zoneSizeKilobytes: number };

const centers = ref<Center[]>([]);
const selectedCenterId = ref<string>();
const activeProtocol = ref<'http' | 'stream'>('http');
const loading = ref(false);
const httpUpstreams = ref<HttpUpstream[]>([]);
const streamUpstreams = ref<StreamUpstream[]>([]);
const drawerOpen = ref(false);
const editingId = ref<string>();
const targetDrawerOpen = ref(false);
const selectedHttpUpstream = ref<HttpUpstream>();
const editingTargetId = ref<string>();
const freshTarget = () => ({ targetHost: '', targetPort: 8080, weight: 1, maxFails: 3, failTimeoutSeconds: 10, resolveEnabled: false, backup: false, enabled: true });
const targetForm = ref(freshTarget());
const healthResults = ref<HealthResult[]>([]);
const healthChecking = ref(false);

const freshHttp = () => ({ keepaliveConnections: 32, zoneSizeKilobytes: 64, name: '', healthCheckEnabled: false, healthCheckPath: '/health', healthCheckIntervalSeconds: 10, healthCheckTimeoutMilliseconds: 1000, healthCheckExpectedStatus: 200 });
const freshStream = () => ({ name: '', targetHost: '', targetPort: 3306, resolveEnabled: false, zoneSizeKilobytes: 64 });
const form = ref(freshHttp() as ReturnType<typeof freshHttp> | ReturnType<typeof freshStream>);
const currentList = computed(() => activeProtocol.value === 'http' ? httpUpstreams.value : streamUpstreams.value);
const selectedCenter = computed(() => centers.value.find((item) => item.id === selectedCenterId.value));
const title = computed(() => activeProtocol.value === 'http' ? 'HTTP Upstream' : 'Stream Upstream');
const targetColumns = [
  { dataIndex: 'targetHost', key: 'targetHost', title: '地址' },
  { dataIndex: 'targetPort', key: 'targetPort', title: '端口', width: 86 },
  { dataIndex: 'weight', key: 'weight', title: '权重', width: 76 },
  { key: 'health', title: '健康检查', width: 155 },
  { key: 'status', title: '状态', width: 105 },
  { key: 'action', title: '操作', width: 120 },
];
const columns = computed(() => activeProtocol.value === 'http'
  ? [
      { dataIndex: 'name', key: 'name', title: '服务标识' },
      { dataIndex: 'keepaliveConnections', key: 'keepaliveConnections', title: '长连接保留数' },
      { dataIndex: 'targets', key: 'targets', title: '后端实例' },
      { key: 'action', title: '操作', width: 150 },
    ]
  : [
      { dataIndex: 'name', key: 'name', title: '服务标识' },
      { dataIndex: 'targetHost', key: 'targetHost', title: '目标地址' },
      { dataIndex: 'targetPort', key: 'targetPort', title: '目标端口' },
      { key: 'action', title: '操作', width: 150 },
    ]);

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail || body.message || '请求失败');
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

async function loadCenter(centerId: string) {
  selectedCenterId.value = centerId;
  loading.value = true;
  try {
    [httpUpstreams.value, streamUpstreams.value] = await Promise.all([
      request<HttpUpstream[]>(`/centers/${centerId}/http/upstreams`),
      request<StreamUpstream[]>(`/centers/${centerId}/stream/upstreams`),
    ]);
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载 Upstream 失败');
  } finally {
    loading.value = false;
  }
}

async function load() {
  loading.value = true;
  try {
    centers.value = await request<Center[]>('/centers');
    if (centers.value[0]) await loadCenter(centers.value[0].id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载中心失败');
  } finally {
    loading.value = false;
  }
}

function openDrawer(value?: HttpUpstream | StreamUpstream) {
  editingId.value = value?.id;
  if (activeProtocol.value === 'http') {
    const item = value as HttpUpstream | undefined;
    form.value = item ? { keepaliveConnections: item.keepaliveConnections, zoneSizeKilobytes: item.zoneSizeKilobytes, name: item.name, healthCheckEnabled: item.healthCheckEnabled, healthCheckPath: item.healthCheckPath, healthCheckIntervalSeconds: item.healthCheckIntervalSeconds, healthCheckTimeoutMilliseconds: item.healthCheckTimeoutMilliseconds, healthCheckExpectedStatus: item.healthCheckExpectedStatus } : freshHttp();
  } else {
    const item = value as StreamUpstream | undefined;
    form.value = item ? { name: item.name, targetHost: item.targetHost, targetPort: item.targetPort, resolveEnabled: item.resolveEnabled, zoneSizeKilobytes: item.zoneSizeKilobytes } : freshStream();
  }
  drawerOpen.value = true;
}

async function save() {
  if (!selectedCenterId.value) return;
  const base = activeProtocol.value === 'http' ? 'http/upstreams' : 'stream/upstreams';
  try {
    await request(`/centers/${selectedCenterId.value}/${base}${editingId.value ? `/${editingId.value}` : ''}`, {
      body: JSON.stringify(form.value),
      method: editingId.value ? 'PUT' : 'POST',
    });
    drawerOpen.value = false;
    await loadCenter(selectedCenterId.value);
    message.success(editingId.value ? `${title.value} 已更新` : `${title.value} 已创建`);
  } catch (error) {
    message.error(error instanceof Error ? error.message : `保存${title.value}失败`);
  }
}

async function remove(id: string) {
  if (!selectedCenterId.value) return;
  const base = activeProtocol.value === 'http' ? 'http/upstreams' : 'stream/upstreams';
  try {
    await request(`/centers/${selectedCenterId.value}/${base}/${id}`, { method: 'DELETE' });
    await loadCenter(selectedCenterId.value);
    message.success(`${title.value} 已删除`);
  } catch (error) {
    message.error(error instanceof Error ? error.message : `删除${title.value}失败`);
  }
}

function openTargets(upstream: HttpUpstream, target?: HttpTarget) {
  selectedHttpUpstream.value = upstream;
  editingTargetId.value = target?.id;
  targetForm.value = target ? { ...target } : freshTarget();
  healthResults.value = [];
  targetDrawerOpen.value = true;
}
async function runHealthChecks() {
  if (!selectedCenterId.value || !selectedHttpUpstream.value) return;
  healthChecking.value = true;
  try {
    healthResults.value = await request<HealthResult[]>(`/centers/${selectedCenterId.value}/http/upstreams/${selectedHttpUpstream.value.id}/health-checks`);
    message.success('健康检查已完成');
  } catch (error) { message.error(error instanceof Error ? error.message : '健康检查失败'); }
  finally { healthChecking.value = false; }
}
function addTarget() {
  editingTargetId.value = undefined;
  targetForm.value = freshTarget();
}
function editTarget(target: HttpTarget) {
  editingTargetId.value = target.id;
  targetForm.value = { ...target };
}
async function refreshSelectedTargets() {
  if (!selectedCenterId.value || !selectedHttpUpstream.value) return;
  const upstreamId = selectedHttpUpstream.value.id;
  await loadCenter(selectedCenterId.value);
  selectedHttpUpstream.value = httpUpstreams.value.find((item) => item.id === upstreamId);
}
async function saveTarget() {
  if (!selectedCenterId.value || !selectedHttpUpstream.value || !targetForm.value.targetHost) return;
  const isEditing = Boolean(editingTargetId.value);
  try {
    const base = `/centers/${selectedCenterId.value}/http/upstreams/${selectedHttpUpstream.value.id}/targets`;
    await request(`${base}${editingTargetId.value ? `/${editingTargetId.value}` : ''}`, { method: editingTargetId.value ? 'PUT' : 'POST', body: JSON.stringify(targetForm.value) });
    await refreshSelectedTargets(); addTarget(); message.success(isEditing ? '后端实例已更新' : '后端实例已添加');
  } catch (error) { message.error(error instanceof Error ? error.message : '保存后端实例失败'); }
}
async function removeTarget(target: HttpTarget) {
  if (!selectedCenterId.value || !selectedHttpUpstream.value) return;
  try { await request(`/centers/${selectedCenterId.value}/http/upstreams/${selectedHttpUpstream.value.id}/targets/${target.id}`, { method: 'DELETE' }); await refreshSelectedTargets(); addTarget(); message.success('后端实例已删除'); } catch (error) { message.error(error instanceof Error ? error.message : '删除后端实例失败'); }
}
async function removeCurrentTarget() {
  const target = selectedHttpUpstream.value?.targets.find((item) => item.id === editingTargetId.value);
  if (target) await removeTarget(target);
}

function switchProtocol(key: string) {
  activeProtocol.value = key as 'http' | 'stream';
}

onMounted(load);
</script>

<template>
  <div class="p-5">
    <a-card :bordered="false" title="Upstream 配置">
      <a-alert
        class="mb-4"
        message="集中维护七层与四层的后端服务组。修改后请到“版本与审计”发布运行时快照；涉及监听端口的变更还需执行原生配置重载。"
        show-icon
        type="info"
      />
      <a-form layout="inline">
        <a-form-item label="配置中心">
          <a-select
            v-model:value="selectedCenterId"
            class="w-72"
            placeholder="请选择中心"
            :options="centers.map((item) => ({ value: item.id, label: `${item.name}（${item.code}）` }))"
            @change="loadCenter"
          />
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="mt-5" :bordered="false">
      <a-tabs :active-key="activeProtocol" @change="switchProtocol">
        <a-tab-pane key="http" tab="HTTP Upstream" />
        <a-tab-pane key="stream" tab="Stream Upstream" />
      </a-tabs>
      <div class="mb-4 flex items-center justify-between">
        <div>
          <div class="text-base font-medium">{{ selectedCenter ? `${selectedCenter.name} 的${title}` : title }}</div>
          <div class="mt-1 text-sm text-gray-500">
            {{ activeProtocol === 'http' ? '用于 HTTP Server 与 Location 的反向代理目标。' : '用于 TCP/UDP Server 的四层转发目标。' }}
          </div>
        </div>
        <a-button :disabled="!selectedCenterId" type="primary" @click="openDrawer()">新增 Upstream</a-button>
      </div>

      <a-table
        v-if="currentList.length || loading"
        :columns="columns"
        :data-source="currentList"
        :loading="loading"
        :pagination="false"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'keepaliveConnections'">
            <a-tag color="blue">{{ record.keepaliveConnections }} 个连接</a-tag>
          </template>
          <template v-else-if="column.key === 'targets'">
            <a-button type="link" @click="openTargets(record)">管理 {{ record.targets?.length || 0 }} 个实例</a-button>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="openDrawer(record)">编辑</a-button>
            <a-popconfirm title="确认删除该 Upstream？已关联的配置可能无法继续转发。" @confirm="remove(record.id)">
              <a-button danger type="link">删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="当前中心还没有此类 Upstream，可从右上角新建。" />
    </a-card>

    <a-drawer v-model:open="drawerOpen" :title="editingId ? `编辑${title}` : `新增${title}`" :width="500">
      <a-form layout="vertical">
        <a-form-item label="服务标识" required>
          <a-input v-model:value="form.name" placeholder="如 order、payment、mysql" />
        </a-form-item>
        <template v-if="activeProtocol === 'http'">
          <a-form-item label="长连接保留数" extra="Nginx 与后端服务之间长期保留的最大空闲连接数。" required>
            <a-input-number v-model:value="form.keepaliveConnections" class="w-full" :max="10000" :min="1" />
          </a-form-item>
          <a-form-item label="动态解析共享内存（KB）" extra="任一后端实例启用 resolve 时自动生成 upstream zone，建议不小于 64KB。" required>
            <a-input-number v-model:value="form.zoneSizeKilobytes" class="w-full" :max="65536" :min="8" />
          </a-form-item>
          <a-form-item label="主动健康检查">
            <a-checkbox v-model:checked="form.healthCheckEnabled">启用控制面探测配置</a-checkbox>
          </a-form-item>
          <template v-if="form.healthCheckEnabled">
            <a-form-item label="健康检查路径" extra="控制面调用每个 HTTP 后端实例时使用的路径。">
              <a-input v-model:value="form.healthCheckPath" placeholder="/health" />
            </a-form-item>
            <div class="grid grid-cols-3 gap-4">
              <a-form-item label="间隔（秒）"><a-input-number v-model:value="form.healthCheckIntervalSeconds" class="w-full" :min="1" :max="3600" /></a-form-item>
              <a-form-item label="超时（毫秒）"><a-input-number v-model:value="form.healthCheckTimeoutMilliseconds" class="w-full" :min="50" :max="60000" /></a-form-item>
              <a-form-item label="期望状态码"><a-input-number v-model:value="form.healthCheckExpectedStatus" class="w-full" :min="100" :max="599" /></a-form-item>
            </div>
          </template>
        </template>
        <template v-else>
          <a-form-item label="目标地址" required>
            <a-input v-model:value="form.targetHost" placeholder="IP 地址或可解析的域名" />
          </a-form-item>
          <a-form-item label="目标端口" required>
            <a-input-number v-model:value="form.targetPort" class="w-full" :max="65535" :min="1" />
          </a-form-item>
          <a-form-item label="动态域名解析" extra="开启后生成 server ... resolve，并自动为 Upstream 定义共享内存区；DNS 地址和缓存时间在 DNS Resolver 菜单配置。">
            <a-checkbox v-model:checked="form.resolveEnabled">启用 resolve 动态解析</a-checkbox>
          </a-form-item>
          <a-form-item v-if="form.resolveEnabled" label="共享内存（KB）" required>
            <a-input-number v-model:value="form.zoneSizeKilobytes" class="w-full" :max="65536" :min="8" />
          </a-form-item>
        </template>
      </a-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button @click="drawerOpen = false">取消</a-button>
          <a-button type="primary" @click="save">保存</a-button>
        </div>
      </template>
    </a-drawer>
    <a-drawer v-model:open="targetDrawerOpen" :title="`管理 ${selectedHttpUpstream?.name || ''} 的 HTTP 后端实例`" :width="720">
      <a-alert class="mb-4" type="info" show-icon message="保存后请在“版本与审计”中生成原生配置并重载，新的后端实例才会参与转发。" />
      <div class="mb-3 flex items-center justify-between"><span class="text-sm text-gray-500">{{ selectedHttpUpstream?.healthCheckEnabled ? `主动检查：${selectedHttpUpstream.healthCheckPath}，每 ${selectedHttpUpstream.healthCheckIntervalSeconds} 秒` : '未启用主动健康检查' }}</span><a-button :disabled="!selectedHttpUpstream?.healthCheckEnabled" :loading="healthChecking" @click="runHealthChecks">立即检查</a-button></div>
      <a-alert v-if="healthResults.length" class="mb-4" :type="healthResults.every((item) => item.status === 'HEALTHY') ? 'success' : 'warning'" show-icon :message="healthResults.map((item) => `${item.targetHost}:${item.targetPort} ${item.status === 'HEALTHY' ? '健康' : item.status === 'NOT_CONFIGURED' ? '未配置' : '异常'}${item.httpStatus ? `（${item.httpStatus}）` : ''}`).join('；')" />
      <a-table class="mb-5" :columns="targetColumns" :data-source="selectedHttpUpstream?.targets || []" :pagination="false" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'health'">{{ record.maxFails }} 次失败 / {{ record.failTimeoutSeconds }} 秒</template>
          <template v-else-if="column.key === 'status'"><a-tag :color="record.enabled ? 'green' : 'default'">{{ record.enabled ? '启用' : '停用' }}</a-tag><a-tag v-if="record.resolveEnabled" color="blue">动态解析（resolve）</a-tag><a-tag v-if="record.backup" color="orange">备用</a-tag></template>
          <template v-else-if="column.key === 'action'"><a-button type="link" @click="editTarget(record)">编辑</a-button><a-popconfirm title="确认删除该后端实例？" @confirm="removeTarget(record)"><a-button danger type="link">删除</a-button></a-popconfirm></template>
        </template>
      </a-table>
      <a-empty v-if="!(selectedHttpUpstream?.targets?.length)" class="mb-5" description="尚未配置后端实例" />
      <div class="mb-3 flex items-center justify-between"><span class="text-base font-medium">{{ editingTargetId ? '编辑后端实例' : '新增后端实例' }}</span><a-button type="link" @click="addTarget">清空并新增</a-button></div>
      <a-form layout="vertical"><a-form-item label="后端地址" required><a-input v-model:value="targetForm.targetHost" placeholder="如 10.0.0.10 或 api.internal" /></a-form-item><div class="grid grid-cols-2 gap-4"><a-form-item label="后端端口" required><a-input-number v-model:value="targetForm.targetPort" class="w-full" :min="1" :max="65535" /></a-form-item><a-form-item label="权重"><a-input-number v-model:value="targetForm.weight" class="w-full" :min="1" :max="1000" /></a-form-item><a-form-item label="最大失败次数"><a-input-number v-model:value="targetForm.maxFails" class="w-full" :min="0" :max="100" /></a-form-item><a-form-item label="失败判定时间（秒）"><a-input-number v-model:value="targetForm.failTimeoutSeconds" class="w-full" :min="1" :max="3600" /></a-form-item></div><a-form-item extra="开启后生成 server 域名:端口 resolve；所属 Upstream 会自动生成 zone 共享内存区。"><a-checkbox v-model:checked="targetForm.resolveEnabled">启用 DNS 动态解析（resolve）</a-checkbox><a-checkbox v-model:checked="targetForm.backup" class="ml-4">作为备用实例</a-checkbox><a-checkbox v-model:checked="targetForm.enabled" class="ml-4">启用实例</a-checkbox></a-form-item></a-form>
      <template #footer><div class="flex justify-end gap-2"><a-popconfirm v-if="editingTargetId" title="确认删除当前后端实例？" @confirm="removeCurrentTarget"><a-button danger>删除当前项</a-button></a-popconfirm><a-button @click="targetDrawerOpen=false">关闭</a-button><a-button type="primary" @click="saveTarget">{{ editingTargetId ? '保存修改' : '添加实例' }}</a-button></div></template>
    </a-drawer>
  </div>
</template>
