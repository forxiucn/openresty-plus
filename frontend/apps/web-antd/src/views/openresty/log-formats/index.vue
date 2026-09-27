<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { Textarea as ATextarea, message } from 'ant-design-vue';

type Center = { id: string; name: string; code: string };
const centers = ref<Center[]>([]);
const centerId = ref<string>();
const httpLogFormat = ref("'$remote_addr - $remote_user [$time_local] \"$request\" $status $body_bytes_sent'");
const streamLogFormat = ref("'$remote_addr [$time_local] $protocol $status $bytes_sent $bytes_received $session_time'");
async function request<T>(path: string, options?: RequestInit): Promise<T> { const response = await fetch(`/api${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(options?.headers || {}) } }); if (!response.ok) throw new Error((await response.text()) || '请求失败'); return response.json() as Promise<T>; }
async function load(id: string) { centerId.value = id; const data = await request<any>(`/centers/${id}/http/settings`); httpLogFormat.value = data.httpLogFormat; streamLogFormat.value = data.streamLogFormat; }
async function save() { if (!centerId.value) return; const current = await request<any>(`/centers/${centerId.value}/http/settings`); await request(`/centers/${centerId.value}/http/settings`, { method: 'PUT', body: JSON.stringify({ ...current, httpLogFormat: httpLogFormat.value, streamLogFormat: streamLogFormat.value }) }); message.success('日志格式已保存'); }
onMounted(async () => { centers.value = await request<Center[]>('/centers'); if (centers.value[0]) await load(centers.value[0].id); });
</script>
<template><div class="ops-page"><a-card :bordered="false" title="log_format 配置"><a-alert type="info" show-icon message="HTTP 与 Stream 使用系统固定的日志格式标识。这里只填写格式内容，不要填写标识、分号或换行。"/><a-form layout="vertical" class="mt-4"><a-form-item label="配置中心"><a-select v-model:value="centerId" :options="centers.map(v => ({ value: v.id, label: `${v.name}（${v.code}）` }))" @change="load"/></a-form-item><a-form-item label="HTTP log_format"><a-textarea v-model:value="httpLogFormat" :auto-size="{ minRows: 3, maxRows: 8 }"/></a-form-item><a-form-item label="Stream log_format"><a-textarea v-model:value="streamLogFormat" :auto-size="{ minRows: 3, maxRows: 8 }"/></a-form-item><a-button type="primary" @click="save">保存日志格式</a-button></a-form></a-card></div></template>
