<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import {
  Alert as AAlert,
  Button as AButton,
  Card as ACard,
  Drawer as ADrawer,
  Empty as AEmpty,
  Form as AForm,
  FormItem as AFormItem,
  Input as AInput,
  InputNumber as AInputNumber,
  Select as ASelect,
  Table as ATable,
  Tabs as ATabs,
  TabPane as ATabPane,
  Tag as ATag,
  message,
} from 'ant-design-vue';

type Center = { id: string; code: string; name: string };
type HttpUpstream = { id: string; name: string; keepaliveConnections: number };
type StreamUpstream = { id: string; name: string; targetHost: string; targetPort: number };

const centers = ref<Center[]>([]);
const selectedCenterId = ref<string>();
const activeProtocol = ref<'http' | 'stream'>('http');
const loading = ref(false);
const httpUpstreams = ref<HttpUpstream[]>([]);
const streamUpstreams = ref<StreamUpstream[]>([]);
const drawerOpen = ref(false);
const editingId = ref<string>();

const freshHttp = () => ({ keepaliveConnections: 32, name: '' });
const freshStream = () => ({ name: '', targetHost: '', targetPort: 3306 });
const form = ref(freshHttp() as ReturnType<typeof freshHttp> | ReturnType<typeof freshStream>);
const currentList = computed(() => activeProtocol.value === 'http' ? httpUpstreams.value : streamUpstreams.value);
const selectedCenter = computed(() => centers.value.find((item) => item.id === selectedCenterId.value));
const title = computed(() => activeProtocol.value === 'http' ? 'HTTP Upstream' : 'Stream Upstream');
const columns = computed(() => activeProtocol.value === 'http'
  ? [
      { dataIndex: 'name', key: 'name', title: '服务标识' },
      { dataIndex: 'keepaliveConnections', key: 'keepaliveConnections', title: '长连接保留数' },
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
    form.value = item ? { keepaliveConnections: item.keepaliveConnections, name: item.name } : freshHttp();
  } else {
    const item = value as StreamUpstream | undefined;
    form.value = item ? { name: item.name, targetHost: item.targetHost, targetPort: item.targetPort } : freshStream();
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
        </template>
        <template v-else>
          <a-form-item label="目标地址" required>
            <a-input v-model:value="form.targetHost" placeholder="IP 地址或可解析的域名" />
          </a-form-item>
          <a-form-item label="目标端口" required>
            <a-input-number v-model:value="form.targetPort" class="w-full" :max="65535" :min="1" />
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
  </div>
</template>
