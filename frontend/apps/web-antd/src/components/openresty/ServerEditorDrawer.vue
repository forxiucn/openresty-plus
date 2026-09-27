<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import {
  Alert as AAlert, Button as AButton, Checkbox as ACheckbox, Col as ACol,
  Drawer as ADrawer, Form as AForm, FormItem as AFormItem, Input as AInput,
  InputNumber as AInputNumber, Menu as AMenu, MenuItem as AMenuItem,
  Popconfirm as APopconfirm, Row as ARow, Select as ASelect, Space as ASpace,
  Switch as ASwitch, Table as ATable, Tag as ATag, message,
} from 'ant-design-vue';
import KeyValueEditor from '#/components/openresty/KeyValueEditor.vue';

type Server = Record<string, any>;
type Location = Record<string, any>;
type Certificate = { id: string; name: string; commonName: string; enabled: boolean };
const props = defineProps<{ open: boolean; centerId?: string; server?: Server; upstreams?: Array<{ id: string; name: string }> }>();
const emit = defineEmits<{ 'update:open': [value: boolean]; saved: [] }>();
const section = ref('basic');
const saving = ref(false);
const form = ref<Server>({});
const serverHeaderValue = ref('');
const locations = ref<Location[]>([]);
const locationOpen = ref(false);
const locationSaving = ref(false);
const editingLocationId = ref<string>();
const locationForm = ref<Location>({});
const certificates = ref<Certificate[]>([]);
const presets = [
  ['X-Frame-Options: SAMEORIGIN', '防点击劫持'],
  ['X-Content-Type-Options: nosniff', '防 MIME 嗅探'],
  ['Strict-Transport-Security: max-age=31536000; includeSubDomains', '强制 HTTPS（HSTS）'],
  ["Content-Security-Policy: default-src 'self'", '内容安全策略（CSP）'],
  ['Referrer-Policy: strict-origin-when-cross-origin', '来源信息保护'],
];
const sections = [
  ['basic', '基本'], ['locations', 'Location 配置'], ['security', '安全配置'],
  ['headers', '响应头'], ['tls', 'HTTPS'], ['other', '其他'],
];
const title = computed(() => form.value.id ? `编辑 ${form.value.domain || 'HTTP Server'}` : '新增 HTTP Server');
const certificateOptions = computed(() => certificates.value
  .filter((certificate) => certificate.enabled)
  .map((certificate) => ({ value: certificate.id, label: `${certificate.name}（${certificate.commonName}）` })));
const locationColumns = [
  { title: '路径', dataIndex: 'path', key: 'path' }, { title: '方式', key: 'action' },
  { title: '目标 Upstream', key: 'upstream' }, { title: '操作', key: 'operation', width: 140 },
];
function clone<T>(value: T): T { return JSON.parse(JSON.stringify(value ?? {})); }
async function api<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(options?.headers || {}) } });
  if (!response.ok) throw new Error((await response.text()) || '请求失败');
  return response.status === 204 ? (undefined as T) : response.json();
}
async function loadLocations() {
  if (!props.centerId || !form.value.id) return;
  const result = await api<any>(`/centers/${props.centerId}/http/servers/${form.value.id}/locations/paged?page=1&size=200`);
  locations.value = result.records || result.content || result.items || result || [];
}
async function resolveServerId() {
  if (!props.centerId || !form.value.id) return form.value.id;
  const servers = await api<Server[]>(`/centers/${props.centerId}/http/servers`);
  const current = servers.find((server) => server.id === form.value.id);
  if (current) return current.id;
  const source = props.server || form.value;
  const replacement = servers.find((server) => server.domain === source.domain && server.listenPort === source.listenPort);
  if (!replacement) throw new Error('HTTP server not found');
  form.value.id = replacement.id;
  return replacement.id;
}
watch(() => [props.open, props.server] as const, async ([open]) => {
  if (!open) return;
  section.value = 'basic'; form.value = { hideVersion: true, responseHeaders: [], sslEnabled: false, listenPort: 80, ...clone(props.server) };
  const existingServerHeader = (form.value.responseHeaders || []).find((header: string) => header.toLowerCase().startsWith('server: '));
  serverHeaderValue.value = existingServerHeader ? existingServerHeader.slice(existingServerHeader.indexOf(':') + 1).trim() : '';
  form.value.responseHeaders = (form.value.responseHeaders || []).filter((header: string) => !header.toLowerCase().startsWith('server: '));
  try { certificates.value = props.centerId ? await api<Certificate[]>(`/centers/${props.centerId}/tls-certificates`) : []; }
  catch { certificates.value = []; }
  await loadLocations();
}, { immediate: true, deep: true });
function close() { emit('update:open', false); }
async function saveServer() {
  if (!props.centerId || !form.value.domain?.trim()) { message.warning('请填写域名'); return; }
  if (form.value.sslEnabled && !form.value.certificateId) { message.warning('启用 TLS 后请选择证书'); return; }
  saving.value = true;
  try {
    const responseHeaders = (form.value.responseHeaders || []).filter((header: string) => !header.toLowerCase().startsWith('server: '));
    if (form.value.hideVersion && serverHeaderValue.value.trim()) responseHeaders.push(`Server: ${serverHeaderValue.value.trim()}`);
    form.value.responseHeaders = responseHeaders;
    const id = await resolveServerId();
    const base = { domain: form.value.domain.trim(), listenPort: form.value.listenPort, sslEnabled: form.value.sslEnabled, certificateId: form.value.certificateId, upstreamId: form.value.upstreamId, accessLog: form.value.accessLog, errorLog: form.value.errorLog };
    const saved = await api<Server>(`/centers/${props.centerId}/http/servers${id ? `/${id}` : ''}`, { method: id ? 'PUT' : 'POST', body: JSON.stringify(base) });
    form.value = { ...form.value, ...saved };
    if (form.value.id) await api(`/centers/${props.centerId}/http/servers/${form.value.id}/directives`, { method: 'PUT', body: JSON.stringify({ rootPath: form.value.rootPath, hideVersion: form.value.hideVersion, responseHeaders: form.value.responseHeaders || [], errorPages: form.value.errorPages || {} }) });
    window.dispatchEvent(new Event('openresty-config-saved')); message.success('Server 草稿已保存，发布后生效'); emit('saved');
  } catch (e) { message.error(e instanceof Error ? e.message : '保存 Server 失败'); }
  finally { saving.value = false; }
}
function openLocation(value?: Location) {
  editingLocationId.value = value?.id;
  locationForm.value = {
    path: '/', action: 'PROXY', methods: ['GET'], contentTypes: [], headerLengthMin: 0, headerLengthMax: 8192,
    bodyLengthMin: 0, bodyLengthMax: 1_048_576, proxyConnectTimeoutMs: 5000, proxyReadTimeoutMs: 60_000, proxySendTimeoutMs: 60_000,
    rateLimitEnabled: false, ratePerSecond: 10, rateLimitBurst: 0, rateLimitNodelay: false, dynamicDnsEnabled: false,
    responseHeaders: [], returnStatus: 200, returnContentTypeMode: 'CUSTOM', returnContentType: 'application/json', ...clone(value),
  };
  locationOpen.value = true;
}
async function saveLocation() {
  if (!props.centerId || !form.value.id || !locationForm.value.path) return;
  locationSaving.value = true;
  try {
    const { action, rootPath, aliasPath, returnStatus, returnBody, returnContentTypeMode, returnContentType, responseHeaders, ...payload } = locationForm.value;
    const saved = await api<Location>(`/centers/${props.centerId}/http/servers/${form.value.id}/locations${editingLocationId.value ? `/${editingLocationId.value}` : ''}`, { method: editingLocationId.value ? 'PUT' : 'POST', body: JSON.stringify(payload) });
    await api(`/centers/${props.centerId}/http/servers/${form.value.id}/locations/${saved.id}/directives`, { method: 'PUT', body: JSON.stringify({ action, rootPath, aliasPath, returnStatus, returnBody, returnContentTypeMode, returnContentType, responseHeaders }) });
    locationOpen.value = false; await loadLocations(); message.success('Location 草稿已保存'); emit('saved');
  } catch (e) { message.error(e instanceof Error ? e.message : '保存 Location 失败'); }
  finally { locationSaving.value = false; }
}
async function removeLocation(id: string) {
  if (!props.centerId || !form.value.id) return;
  try { await api(`/centers/${props.centerId}/http/servers/${form.value.id}/locations/${id}`, { method: 'DELETE' }); await loadLocations(); message.success('Location 已删除'); emit('saved'); }
  catch (e) { message.error(e instanceof Error ? e.message : '删除 Location 失败'); }
}
function togglePreset(value: string, checked: boolean) { const items = form.value.responseHeaders || []; form.value.responseHeaders = checked ? [...new Set([...items, value])] : items.filter((item: string) => item !== value); }
function upstreamName(id?: string) { return props.upstreams?.find(item => item.id === id)?.name || '—'; }
</script>

<template>
  <a-drawer :open="open" :title="title" :width="920" destroy-on-close @update:open="emit('update:open', $event)">
    <div class="editor-layout">
      <a-menu :selected-keys="[section]" mode="inline" class="editor-menu" @click="({ key }: { key: string }) => section = key">
        <a-menu-item v-for="item in sections" :key="item[0]">{{ item[1] }}</a-menu-item>
      </a-menu>
      <section class="editor-content">
        <template v-if="section === 'basic'">
          <a-form layout="vertical"><a-row :gutter="16">
            <a-col :span="16"><a-form-item label="域名" required extra="server_name：该 Server 匹配的请求域名；可填写域名、通配符或 IP。"><a-input v-model:value="form.domain" placeholder="api.example.com"/></a-form-item></a-col>
            <a-col :span="8"><a-form-item label="监听端口" required extra="listen：OpenResty 接收请求的 TCP 端口，范围为 1 至 65535。"><a-input-number v-model:value="form.listenPort" :min="1" :max="65535" class="w-full"/></a-form-item></a-col>
            <a-col :span="12"><a-form-item label="默认 Upstream" extra="proxy_pass：没有由 Location 覆盖时，转发到的默认后端服务组。"><a-select v-if="upstreams?.length" v-model:value="form.upstreamId" allow-clear :options="upstreams.map(v => ({ value: v.id, label: v.name }))"/><a-input v-else v-model:value="form.upstreamId" placeholder="请输入 Upstream ID，或从 Upstream 菜单维护"/></a-form-item></a-col>
            <a-col :span="12"><a-form-item label="网站 root 目录" extra="root：静态文件的根目录；Location 的 root 或 alias 会覆盖此处。"><a-input v-model:value="form.rootPath" placeholder="/srv/openresty-content/site-a"/></a-form-item></a-col>
            <a-col :span="12"><a-form-item label="访问日志" extra="access_log：访问请求记录的文件路径；留空使用实例默认路径。"><a-input v-model:value="form.accessLog" placeholder="留空使用默认路径"/></a-form-item></a-col>
            <a-col :span="12"><a-form-item label="错误日志" extra="error_log：运行错误记录的文件路径；留空使用实例默认路径。"><a-input v-model:value="form.errorLog" placeholder="留空使用默认路径"/></a-form-item></a-col>
          </a-row><a-form-item><a-switch v-model:checked="form.sslEnabled" checked-children="启用 TLS" un-checked-children="关闭 TLS"/></a-form-item></a-form>
        </template>
        <template v-else-if="section === 'locations'">
          <a-alert v-if="!form.id" type="info" show-icon message="请先保存 Server，再维护其 Location。"/>
          <template v-else><div class="mb-3 flex justify-between"><span>当前 Server 下的 Location</span><a-button type="primary" @click="openLocation()">新增 Location</a-button></div>
            <a-table :columns="locationColumns" :data-source="locations" row-key="id" :pagination="false"><template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'"><a-tag :color="record.action === 'PROXY' ? 'blue' : record.action === 'STATIC' ? 'green' : 'orange'">{{ record.action === 'PROXY' ? '代理转发' : record.action === 'STATIC' ? '静态文件' : '直接返回' }}</a-tag></template>
              <template v-else-if="column.key === 'upstream'">{{ record.action === 'PROXY' ? upstreamName(record.upstreamId) : record.action === 'STATIC' ? (record.aliasPath || record.rootPath || '—') : `HTTP ${record.returnStatus || 200}` }}</template>
              <template v-else-if="column.key === 'operation'"><a-space :size="0"><a-button type="link" @click="openLocation(record)">编辑</a-button><a-popconfirm title="确认删除此 Location？" @confirm="removeLocation(record.id)"><a-button type="link" danger>删除</a-button></a-popconfirm></a-space></template>
            </template></a-table></template>
        </template>
        <template v-else-if="section === 'security'"><a-alert class="mb-4" type="info" show-icon message="add_header：向响应添加安全头；启用后每项都会自动追加 always，4xx 与 5xx 响应同样携带该头。"/><a-space direction="vertical" fill><a-checkbox v-for="item in presets" :key="item[0]" :checked="(form.responseHeaders || []).includes(item[0])" @change="(e: any) => togglePreset(item[0], e.target.checked)">{{ item[1] }}（{{ item[0] }}）</a-checkbox></a-space></template>
        <template v-else-if="section === 'headers'"><a-form layout="vertical"><a-form-item label="自定义响应头" extra="每项为一组 Header 名称和值，渲染时自动追加 always。"><key-value-editor v-model="form.responseHeaders"/></a-form-item></a-form></template>
        <template v-else-if="section === 'tls'"><a-alert type="info" show-icon message="ssl_certificate：开启 TLS 后选择当前中心已启用的证书；发布并重载原生配置后生效。"/><a-form layout="vertical" class="mt-4"><a-form-item v-if="form.sslEnabled" label="TLS 证书" required extra="ssl_certificate / ssl_certificate_key：选择证书管理中的启用证书。"><a-select v-model:value="form.certificateId" show-search :options="certificateOptions" placeholder="请选择证书"/></a-form-item><a-alert v-else type="warning" show-icon message="请先在“基本”中开启 TLS，才能绑定证书。"/><a-alert v-if="form.sslEnabled && !certificateOptions.length" class="mt-3" type="warning" show-icon message="当前中心没有已启用证书，请先在“证书管理”中新增或启用证书。"/></a-form></template>
        <template v-else><a-form layout="vertical"><a-form-item extra="server_tokens：关闭后响应头和错误页不再显示 OpenResty / Nginx 版本号。"><a-checkbox v-model:checked="form.hideVersion">隐藏 OpenResty / Nginx 版本号</a-checkbox></a-form-item><a-form-item v-if="form.hideVersion" label="自定义 Server 响应头" extra="Server：替换默认 Server 响应头的值；留空则完全移除该响应头。"><a-input v-model:value="serverHeaderValue" placeholder="例如：Gateway"/></a-form-item></a-form></template>
      </section>
    </div>
    <template #footer><div class="flex justify-end gap-2"><a-button @click="close">取消</a-button><a-button type="primary" :loading="saving" @click="saveServer">保存 Server</a-button></div></template>
  </a-drawer>
  <a-drawer v-model:open="locationOpen" :title="editingLocationId ? '编辑 Location' : '新增 Location'" :width="760" destroy-on-close>
    <a-form layout="vertical"><a-row :gutter="16"><a-col :span="12"><a-form-item label="匹配路径" required extra="location：只匹配此前缀下的 URI，例如 /api/ 或 /health。"><a-input v-model:value="locationForm.path"/></a-form-item></a-col><a-col :span="12"><a-form-item label="处理方式" extra="决定当前路径执行代理、读取静态文件还是直接返回内容。"><a-select v-model:value="locationForm.action" :options="[{value:'PROXY',label:'代理转发'},{value:'STATIC',label:'静态文件'},{value:'RETURN',label:'直接返回'}]"/></a-form-item></a-col></a-row>
      <a-form-item v-if="locationForm.action === 'PROXY'" label="转发 Upstream" extra="proxy_pass：选择当前请求应转发到的后端服务组。"><a-select v-if="upstreams?.length" v-model:value="locationForm.upstreamId" :options="upstreams.map(v => ({ value: v.id, label: v.name }))"/><a-input v-else v-model:value="locationForm.upstreamId" placeholder="请输入 Upstream ID"/></a-form-item>
      <a-row v-if="locationForm.action === 'STATIC'" :gutter="16"><a-col :span="12"><a-form-item label="root 目录" extra="root：请求 URI 会拼接到此目录；与 alias 二选一。"><a-input v-model:value="locationForm.rootPath"/></a-form-item></a-col><a-col :span="12"><a-form-item label="alias 目录" extra="alias：用该目录替换匹配路径；与 root 二选一。"><a-input v-model:value="locationForm.aliasPath"/></a-form-item></a-col></a-row>
      <a-row v-if="locationForm.action === 'RETURN'" :gutter="16"><a-col :span="6"><a-form-item label="状态码" extra="return：填写 100 至 599 的 HTTP 状态码。"><a-input-number v-model:value="locationForm.returnStatus" :min="100" :max="599" class="w-full"/></a-form-item></a-col><a-col :span="18"><a-form-item label="返回内容" extra="return：客户端会收到的固定响应正文。"><a-input v-model:value="locationForm.returnBody"/></a-form-item></a-col></a-row>
      <a-form-item label="请求方法" extra="只允许所选 HTTP 方法命中该 Location，例如 GET、POST。"><a-select v-model:value="locationForm.methods" mode="tags" placeholder="GET、POST 等"/></a-form-item><a-form-item label="响应头" extra="add_header：每项由 Header 名称和值组成，发布时自动追加 always。"><key-value-editor v-model="locationForm.responseHeaders"/></a-form-item>
    </a-form><template #footer><div class="flex justify-end gap-2"><a-button @click="locationOpen=false">取消</a-button><a-button type="primary" :loading="locationSaving" @click="saveLocation">保存 Location</a-button></div></template>
  </a-drawer>
</template>
<style scoped>
.editor-layout { display:grid; grid-template-columns:150px minmax(0,1fr); gap:24px; min-height:480px; }
.editor-menu { border-inline-end:1px solid var(--ant-color-border-secondary); }
.editor-content { min-width:0; padding:4px 0; }
@media (max-width: 768px) { .editor-layout { grid-template-columns:1fr; } .editor-menu { border:0; } }
</style>
