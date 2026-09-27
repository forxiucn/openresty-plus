<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  Alert as AAlert,
  Button as AButton,
  Card as ACard,
  Checkbox as ACheckbox,
  Col as ACol,
  Form as AForm,
  FormItem as AFormItem,
  Input as AInput,
  InputNumber as AInputNumber,
  Menu as AMenu,
  MenuItem as AMenuItem,
  Row as ARow,
  Select as ASelect,
  Tabs as ATabs,
  TabPane as ATabPane,
  message,
  notification,
} from 'ant-design-vue';
import KeyValueEditor from '#/components/openresty/KeyValueEditor.vue';
type Server = {
  id: string;
  domain: string;
  listenPort: number;
  sslEnabled: boolean;
  certificateId?: string;
  upstreamId?: string;
  accessLog?: string;
  errorLog?: string;
  rootPath?: string;
  hideVersion: boolean;
  responseHeaders: string[];
  errorPages?: Record<string, string>;
};
type Center = { id: string; name: string; code: string };
type Certificate = { id: string; name: string; commonName: string };
const route = useRoute(),
  router = useRouter(),
  server = ref<Server>(),
  centers = ref<Center[]>([]),
  certificates = ref<Certificate[]>([]),
  selectedSection = ref<string[]>(['domain']),
  centerId = String(route.query.centerId || ''),
  tab = ref('basic'),
  savingIdentity = ref(false);
let serverId = String(route.query.serverId || '');
const section = computed(() => selectedSection.value[0] || 'domain');
const labels: { key: string; label: string }[] = [
  ['domain', '域名设置'],
  ['root', '网站目录'],
  ['index', '默认文档'],
  ['rate', '流量限制'],
  ['proxy', 'Location 配置'],
  ['access', '密码访问'],
  ['tls', 'HTTPS'],
  ['static', '伪静态'],
  ['referer', '防盗链'],
  ['redirect', '重定向'],
  ['security', '安全配置'],
  ['headers', '响应头'],
  ['other', '其他'],
].map(([key, label]) => ({ key, label }));
const securityHeaders = [
  { key: 'X-Frame-Options: SAMEORIGIN', label: '防点击劫持' },
  { key: 'X-Content-Type-Options: nosniff', label: '防 MIME 嗅探' },
  {
    key: 'Strict-Transport-Security: max-age=31536000; includeSubDomains',
    label: '强制 HTTPS（HSTS）',
  },
  { key: "Content-Security-Policy: default-src 'self'", label: '内容安全策略（CSP）' },
  { key: 'Referrer-Policy: strict-origin-when-cross-origin', label: 'Referrer 隐私策略' },
];
const customHeaders = ref<string[]>([]);
const logLines = ref<string[]>([]), logType = ref<'access' | 'error'>('access'), logConnected = ref(false);
const logNodeId = ref(String(route.query.nodeId || ''));
let logStream: EventSource | undefined;
async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const r = await fetch(`/api${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
  });
  if (!r.ok) {
    const detail = await r.text();
    throw new Error(detail || `请求失败（${r.status}）`);
  }
  return r.status === 204 ? (undefined as T) : (r.json() as Promise<T>);
}
async function load() {
  try {
    const [all, centerList, certificateList, nodeList] = await Promise.all([
      request<Server[]>(`/centers/${centerId}/http/servers`),
      request<Center[]>('/centers'),
      request<Certificate[]>(`/centers/${centerId}/tls-certificates`),
      request<Array<{ id: string; enabled?: boolean }>>(`/centers/${centerId}/nodes`),
    ]);
    if (!logNodeId.value) {
      logNodeId.value = nodeList.find((node) => node.enabled !== false)?.id || nodeList[0]?.id || '';
    }
    centers.value = centerList;
    certificates.value = certificateList;
    server.value = all.find((v) => v.id === serverId);
    if (!server.value) {
      message.error('未找到该 Server，请返回列表重新选择');
      return;
    }
    customHeaders.value = (server.value.responseHeaders || []).filter(
      (value) => !securityHeaders.some((item) => item.key === value),
    );
  } catch {
    message.error('加载 Server 失败');
  }
}
async function save() {
  if (!server.value) return;
  try {
    await saveDirectives();
    window.dispatchEvent(new Event('openresty-config-saved'));
    message.success('Server 配置已保存，发布后生效');
  } catch (error) {
    if (isServerNotFound(error) && (await refreshServerId())) {
      try {
        await saveDirectives();
        window.dispatchEvent(new Event('openresty-config-saved'));
        message.success('Server 配置已保存，发布后生效');
        return;
      } catch (retryError) {
        error = retryError;
      }
    }
    message.error(error instanceof Error ? error.message : '保存失败');
  }
}
async function saveDirectives() {
  if (!server.value) return;
  await request(`/centers/${centerId}/http/servers/${serverId}/directives`, {
    method: 'PUT',
    body: JSON.stringify({
      rootPath: server.value.rootPath,
      hideVersion: server.value.hideVersion,
      responseHeaders: server.value.responseHeaders,
      errorPages: server.value.errorPages || {},
    }),
  });
}
function isServerNotFound(error: unknown) {
  return error instanceof Error && error.message.includes('HTTP server not found');
}
async function refreshServerId() {
  if (!server.value) return false;
  const all = await request<Server[]>(`/centers/${centerId}/http/servers`);
  const current = all.find((item) => item.id === serverId)
    || all.find((item) => item.domain === server.value?.domain && item.listenPort === server.value?.listenPort);
  if (!current) return false;
  if (current.id !== serverId) {
    serverId = current.id;
    await router.replace({ query: { ...route.query, serverId } });
  }
  return true;
}
async function saveIdentity() {
  if (!server.value) return;
  if (!server.value.domain?.trim()) {
    message.warning('请填写域名');
    return;
  }
  if (server.value.sslEnabled && !server.value.certificateId) {
    notification.warning({
      message: '请选择 TLS 证书',
      description: '启用 TLS 的 Server 必须绑定证书后才能保存。',
      placement: 'topRight',
    });
    return;
  }
  savingIdentity.value = true;
  try {
    if (!(await refreshServerId())) {
      throw new Error('HTTP server not found');
    }
    const updated = await request<Server>(`/centers/${centerId}/http/servers/${serverId}`, {
      method: 'PUT',
      body: JSON.stringify({
        domain: server.value.domain.trim(),
        listenPort: server.value.listenPort,
        sslEnabled: server.value.sslEnabled,
        certificateId: server.value.certificateId,
        upstreamId: server.value.upstreamId,
        accessLog: server.value.accessLog,
        errorLog: server.value.errorLog,
      }),
    });
    server.value = { ...server.value, ...updated };
    window.dispatchEvent(new Event('openresty-config-saved'));
    notification.success({
      message: '域名与监听端口已保存',
      description: '配置将在发布并重载后生效。',
      placement: 'topRight',
    });
  } catch (error) {
    notification.error({
      message: '保存域名失败',
      description: error instanceof Error ? error.message : '请检查域名、监听端口与 TLS 证书配置。',
      placement: 'topRight',
    });
  } finally {
    savingIdentity.value = false;
  }
}
function goLocations() {
  router.push({
    path: '/openresty/http-locations',
    query: { centerId, serverId, nodeId: route.query.nodeId },
  });
}
function toggleHeader(key: string, enabled: boolean) {
  if (!server.value) return;
  server.value.responseHeaders = enabled
    ? [...server.value.responseHeaders.filter((value) => value !== key), key]
    : server.value.responseHeaders.filter((value) => value !== key);
}
function saveHeaders() {
  if (!server.value) return;
  server.value.responseHeaders = [
    ...securityHeaders
      .filter((v) => server.value!.responseHeaders.includes(v.key))
      .map((v) => v.key),
    ...customHeaders.value,
  ];
  save();
}
function connectLogs() {
  logStream?.close(); logLines.value = []; logConnected.value = false;
  if (!logNodeId.value) return;
  logStream = new EventSource(`/api/centers/${centerId}/nodes/${logNodeId.value}/logs/stream`);
  logStream.addEventListener('ready', () => { logConnected.value = true; });
  logStream.addEventListener('log', (event) => {
    const item = JSON.parse((event as MessageEvent).data) as { type?: string; path?: string; message?: string; timestamp?: string; };
    const expected = logType.value === 'access' ? server.value?.accessLog : server.value?.errorLog;
    if (item.type && item.type !== logType.value) return;
    if (expected && item.path && item.path !== expected) return;
    logLines.value = [...logLines.value, `${item.timestamp || ''} ${item.message || ''}`.trim()].slice(-300);
  });
  logStream.onerror = () => { logConnected.value = false; };
}
function changeTab(value: string) { tab.value = value; if (value === 'logs') connectLogs(); else logStream?.close(); }
onMounted(load);
onUnmounted(() => logStream?.close());
const title = computed(() =>
  server.value ? `${server.value.domain}:${server.value.listenPort}` : 'Server 配置',
);
const centerTitle = computed(() => {
  const center = centers.value.find((item) => item.id === centerId);
  return center ? `${center.name}（${center.code}）` : '未识别中心';
});
</script>
<template>
  <div class="ops-page">
    <a-card :bordered="false"
      ><div class="server-header">
        <a-button type="link" @click="router.back()">← 返回</a-button><strong>{{ title }}</strong
        ><a-tabs :active-key="tab" @change="changeTab"
          ><a-tab-pane key="basic" tab="基本" /><a-tab-pane key="logs" tab="日志" /></a-tabs>
      </div>
      <div class="server-layout">
        <a-menu
          v-model:selected-keys="selectedSection"
          mode="inline"
          class="server-menu"
          @click="({ key }: any) => key === 'proxy' && goLocations()"
          ><a-menu-item v-for="item in labels" :key="item.key">{{
            item.label
          }}</a-menu-item></a-menu
        >
        <main><template v-if="tab === 'logs'"><a-card size="small" title="Server 实时日志"><a-space class="mb-4"><a-select v-model:value="logType" :options="[{ value: 'access', label: '访问日志' }, { value: 'error', label: '错误日志' }]" @change="connectLogs"/><a-tag :color="logConnected ? 'green' : 'orange'">{{ logConnected ? 'Kafka 实时连接' : '等待日志事件' }}</a-tag></a-space><pre class="server-log-console">{{ logLines.length ? logLines.join('\n') : '暂无日志事件' }}</pre></a-card></template><template v-else>
          <a-alert
            class="mb-4"
            type="info"
            show-icon
            :message="`当前中心：${centerTitle}；当前 Server：${title}`"
          /><template v-if="section === 'domain' && server"
            ><a-form layout="vertical"
              ><a-row :gutter="16"
                ><a-col :span="16"
                  ><a-form-item label="域名" required
                    ><a-input
                      v-model:value="server.domain"
                      placeholder="api.example.com" /></a-form-item></a-col
                ><a-col :span="8"
                  ><a-form-item label="监听端口" required
                    ><a-input-number
                      v-model:value="server.listenPort"
                      :min="1"
                      :max="65535"
                      class="w-full" /></a-form-item></a-col></a-row
              ><a-form-item
                ><a-checkbox v-model:checked="server.sslEnabled">启用 TLS</a-checkbox></a-form-item
              ><a-form-item v-if="server.sslEnabled" label="TLS 证书" required
                ><a-select
                  v-model:value="server.certificateId"
                  placeholder="请选择证书"
                  :options="
                    certificates.map((item) => ({
                      value: item.id,
                      label: `${item.name}（${item.commonName}）`,
                    }))
                  " /></a-form-item
              ><a-button type="primary" :loading="savingIdentity" @click="saveIdentity"
                >保存域名与端口</a-button
              ></a-form
            ></template
          ><template v-else-if="section === 'root'"
            ><a-form layout="vertical"
              ><a-form-item label="root 内容目录"
                ><a-input
                  v-model:value="server!.rootPath"
                  placeholder="/srv/openresty-content/site-a" /></a-form-item
              ><a-button type="primary" @click="save">保存</a-button></a-form
            ></template
          ><template v-else-if="section === 'tls'"
            ><a-alert
              type="info"
              show-icon
              message="TLS 证书与监听配置由 Server 基础字段管理；证书可在创建或编辑 Server 时选择。" /></template
          ><template v-else-if="section === 'proxy'"
            ><a-button type="primary" @click="goLocations">管理 Location 与 API</a-button></template
          ><template v-else-if="section === 'security'"
            ><a-card size="small" title="预制安全响应头"
              ><a-space direction="vertical" fill
                ><a-checkbox
                  v-for="item in securityHeaders"
                  :key="item.key"
                  :checked="server?.responseHeaders.includes(item.key)"
                  @change="(e: any) => toggleHeader(item.key, e.target.checked)"
                  >{{ item.label }}（{{ item.key }}）</a-checkbox
                ></a-space
              >
              <div class="mt-4">
                <a-button type="primary" @click="save">保存安全配置</a-button>
              </div></a-card
            ></template
          ><template v-else-if="section === 'headers'"
            ><a-form layout="vertical"
              ><a-form-item
                label="自定义响应头"
                extra="每行一个 Header-Name: value，渲染时自动附加 always。"
                ><key-value-editor v-model="customHeaders" /></a-form-item
              ><a-button
                type="primary"
                @click="
                  server!.responseHeaders = [
                    ...securityHeaders
                      .filter((v) => server!.responseHeaders.includes(v.key))
                      .map((v) => v.key),
                    ...customHeaders,
                  ];
                  save();
                "
                >保存自定义响应头</a-button
              ></a-form
            ></template
          ><template v-else-if="section === 'other'"
            ><a-form layout="vertical"
              ><a-form-item
                ><a-checkbox v-model:checked="server!.hideVersion"
                  >隐藏版本并清除 Server 响应头</a-checkbox
                ></a-form-item
              ><a-button type="primary" @click="save">保存</a-button></a-form
            ></template
          ><template v-else
            ><a-alert
              type="info"
              show-icon
              :message="`${labels.find((v) => v.key === section)?.label} 将在对应 Location 配置中维护；点击“反向代理”可进入路由配置。`"
          /></template>
        </template></main></div
    ></a-card>
  </div>
</template>
<style scoped>
.server-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.server-header :deep(.ant-tabs) {
  margin-left: 12px;
}
.server-layout {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 24px;
  margin-top: 16px;
}
.server-menu {
  border-inline-end: 1px solid var(--ant-color-border-secondary);
}
main {
  min-height: 420px;
  padding: 8px 12px;
}
.server-log-console {
  min-height: 360px;
  max-height: 620px;
  overflow: auto;
  margin: 0;
  padding: 16px;
  border-radius: 8px;
  background: #111827;
  color: #d1fae5;
  font: 12px/1.65 ui-monospace, SFMono-Regular, Menlo, monospace;
  white-space: pre-wrap;
  word-break: break-word;
}
@media (max-width: 768px) {
  .server-layout {
    grid-template-columns: 1fr;
  }
  .server-menu {
    border: 0;
  }
}
</style>
