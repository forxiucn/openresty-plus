<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import {
  Button as AButton, Card as ACard, Drawer as ADrawer, Form as AForm,
  FormItem as AFormItem, Input as AInput, Textarea as ATextarea, Popconfirm as APopconfirm,
  Select as ASelect, Table as ATable, Tag as ATag,
  Upload as AUpload, message,
} from 'ant-design-vue';
import SectionHelp from '#/components/openresty/SectionHelp.vue';

type Center = { id: string; name: string; code: string };
type Certificate = { id: string; name: string; commonName: string; enabled: boolean; createdAt: string; updatedAt: string };
type Field = 'certificatePem' | 'privateKeyPem' | 'chainPem';

const centers = ref<Center[]>([]);
const centerId = ref<string>();
const rows = ref<Certificate[]>([]);
const drawerOpen = ref(false);
const editing = ref<Certificate>();
const form = ref({ name: '', certificatePem: '', privateKeyPem: '', chainPem: '' });
const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '通用名称（CN）', dataIndex: 'commonName', key: 'commonName' },
  { title: '状态', key: 'enabled', width: 100 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'action', width: 150 },
];
const centerOptions = computed(() => centers.value.map((value) => ({ value: value.id, label: `${value.name}（${value.code}）` })));

async function api<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(options?.headers || {}) } });
  if (!response.ok) { const body = await response.json().catch(() => ({})); throw new Error(body.detail || body.message || '请求失败'); }
  return response.status === 204 ? (undefined as T) : response.json();
}
async function load(id = centerId.value) {
  if (!id) return;
  centerId.value = id;
  try { rows.value = await api<Certificate[]>(`/centers/${id}/tls-certificates`); }
  catch (error) { message.error(error instanceof Error ? error.message : '加载证书失败'); }
}
function reset(value?: Certificate) {
  editing.value = value;
  form.value = value
    ? { name: value.name, certificatePem: '', privateKeyPem: '', chainPem: '' }
    : { name: '', certificatePem: '', privateKeyPem: '', chainPem: '' };
  drawerOpen.value = true;
}
function readFile(field: Field, file: File) {
  const reader = new FileReader();
  reader.onload = () => { form.value[field] = String(reader.result || ''); };
  reader.onerror = () => message.error(`读取 ${file.name} 失败`);
  reader.readAsText(file);
  return false;
}
async function save() {
  if (!centerId.value || !form.value.name.trim()) { message.warning('请填写证书名称'); return; }
  const creating = !editing.value;
  if (creating && (!form.value.certificatePem.trim() || !form.value.privateKeyPem.trim())) { message.warning('请提供证书内容和私钥内容'); return; }
  const payload: Record<string, unknown> = { ...form.value, name: form.value.name.trim() };
  if (!creating && !form.value.certificatePem.trim() && !form.value.privateKeyPem.trim()) {
    delete payload.certificatePem; delete payload.privateKeyPem; delete payload.chainPem;
  }
  try {
    await api(`/centers/${centerId.value}/tls-certificates${editing.value ? `/${editing.value.id}` : ''}`, { method: editing.value ? 'PUT' : 'POST', body: JSON.stringify(payload) });
    drawerOpen.value = false; await load(); window.dispatchEvent(new Event('openresty-config-saved')); message.success(editing.value ? '证书已更新' : '证书已新增');
  } catch (error) { message.error(error instanceof Error ? error.message : '保存证书失败'); }
}
async function remove(item: Certificate) {
  if (!centerId.value) return;
  try { await api(`/centers/${centerId.value}/tls-certificates/${item.id}`, { method: 'DELETE' }); await load(); window.dispatchEvent(new Event('openresty-config-saved')); message.success('证书已删除'); }
  catch (error) { message.error(error instanceof Error ? error.message : '删除证书失败'); }
}
function time(value: string) { return value ? new Date(value).toLocaleString() : '—'; }
onMounted(async () => {
  try { centers.value = await api<Center[]>('/centers'); if (centers.value[0]) await load(centers.value[0].id); }
  catch (error) { message.error(error instanceof Error ? error.message : '加载配置中心失败'); }
});
</script>

<template>
  <div class="ops-page">
    <a-card :bordered="false" title="证书管理">
      <template #extra><section-help text="证书按配置中心隔离保存。启用 TLS 的 HTTP Server 只能选择当前中心已启用的证书；保存后从待发布配置发布并重载原生配置。"/></template>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <a-select v-model:value="centerId" class="w-80" :options="centerOptions" @change="load"/>
        <a-button type="primary" :disabled="!centerId" @click="reset()">新增证书</a-button>
      </div>
    </a-card>
    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="rows" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'"><a-tag :color="record.enabled ? 'green' : 'default'">{{ record.enabled ? '已启用' : '已停用' }}</a-tag></template>
          <template v-else-if="column.key === 'updatedAt'">{{ time(record.updatedAt) }}</template>
          <template v-else-if="column.key === 'action'"><a-button type="link" @click="reset(record)">编辑</a-button><a-popconfirm title="确认删除此证书？已被 TLS Server 使用的证书将导致发布校验失败。" @confirm="remove(record)"><a-button danger type="link">删除</a-button></a-popconfirm></template>
        </template>
      </a-table>
    </a-card>
    <a-drawer v-model:open="drawerOpen" :title="editing ? '编辑证书' : '新增证书'" :width="760" destroy-on-close>
      <a-form layout="vertical">
        <a-form-item label="证书名称" required extra="平台内用于选择和识别的名称，例如生产 API 证书。通用名称（CN）会从证书内容自动读取。"><a-input v-model:value="form.name" placeholder="生产 API 证书"/></a-form-item>
        <a-form-item label="证书内容（PEM）" :required="!editing" extra="ssl_certificate：可直接粘贴 PEM 内容，或上传 .crt、.cer、.pem 文件读取。"><a-upload :show-upload-list="false" accept=".crt,.cer,.pem" :before-upload="(file: File) => readFile('certificatePem', file)"><a-button>上传证书文件</a-button></a-upload><a-textarea v-model:value="form.certificatePem" class="mt-2" :auto-size="{ minRows: 8, maxRows: 16 }" placeholder="-----BEGIN CERTIFICATE-----"/></a-form-item>
        <a-form-item label="私钥内容（PEM）" :required="!editing" extra="ssl_certificate_key：可直接粘贴 PEM 内容，或上传 .key、.pem 文件读取。"><a-upload :show-upload-list="false" accept=".key,.pem" :before-upload="(file: File) => readFile('privateKeyPem', file)"><a-button>上传私钥文件</a-button></a-upload><a-textarea v-model:value="form.privateKeyPem" class="mt-2" :auto-size="{ minRows: 8, maxRows: 16 }" placeholder="-----BEGIN PRIVATE KEY-----"/></a-form-item>
        <a-form-item label="证书链（可选）" extra="中间证书链；上传或粘贴 PEM 内容后会与主证书一起写入 ssl_certificate 文件。"><a-upload :show-upload-list="false" accept=".crt,.cer,.pem" :before-upload="(file: File) => readFile('chainPem', file)"><a-button>上传证书链</a-button></a-upload><a-textarea v-model:value="form.chainPem" class="mt-2" :auto-size="{ minRows: 6, maxRows: 14 }" placeholder="-----BEGIN CERTIFICATE-----"/></a-form-item>
      </a-form>
      <template #footer><div class="flex justify-end gap-2"><a-button @click="drawerOpen = false">取消</a-button><a-button type="primary" @click="save">保存证书</a-button></div></template>
    </a-drawer>
  </div>
</template>
