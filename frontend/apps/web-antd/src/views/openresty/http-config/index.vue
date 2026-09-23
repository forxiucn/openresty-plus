<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import { Alert as AAlert, Button as AButton, Card as ACard, Checkbox as ACheckbox, Col as ACol, Divider as ADivider, Drawer as ADrawer, Empty as AEmpty, Form as AForm, FormItem as AFormItem, Input as AInput, InputNumber as AInputNumber, Row as ARow, Select as ASelect, Space as ASpace, Table as ATable, Tag as ATag, message } from 'ant-design-vue';
type Center={id:string;code:string;name:string}; type DictionaryOption={value:string;label:string}; type Upstream={id:string;name:string;keepaliveConnections:number}; type Certificate={id:string;name:string;commonName:string;enabled:boolean}; type HttpServer={id:string;domain:string;listenPort:number;sslEnabled:boolean;certificateId?:string;upstreamId?:string;accessLog?:string;errorLog?:string}; type HttpLocation={id:string;path:string;methods:string[];contentTypes:string[];headerLengthMin:number;headerLengthMax:number;bodyLengthMin:number;bodyLengthMax:number;upstreamId:string;proxyConnectTimeoutMs:number;proxyReadTimeoutMs:number;proxySendTimeoutMs:number};
const centers=ref<Center[]>([]), upstreams=ref<Upstream[]>([]), servers=ref<HttpServer[]>([]), locations=ref<HttpLocation[]>([]), certificates=ref<Certificate[]>([]); const selectedCenterId=ref<string>(), selectedServerId=ref<string>(), loading=ref(false);
const upstreamOpen=ref(false),serverOpen=ref(false),locationOpen=ref(false),certificateOpen=ref(false); const editingUpstreamId=ref<string>(),editingServerId=ref<string>(),editingLocationId=ref<string>(),editingCertificateId=ref<string>();
const freshUpstream=()=>({name:'',keepaliveConnections:32}); const freshServer=()=>({domain:'',listenPort:80,sslEnabled:false,certificateId:undefined as string|undefined,upstreamId:undefined as string|undefined,accessLog:'',errorLog:''}); const freshCertificate=()=>({name:'',commonName:'',certificatePem:'',privateKeyPem:'',chainPem:'',enabled:true}); const freshLocation=()=>({path:'/',methods:['GET'] as string[],contentTypes:[] as string[],headerLengthMin:0,headerLengthMax:8192,bodyLengthMin:0,bodyLengthMax:1_048_576,upstreamId:undefined as string|undefined,proxyConnectTimeoutMs:5000,proxyReadTimeoutMs:60_000,proxySendTimeoutMs:60_000});
const upstreamForm=ref(freshUpstream()),serverForm=ref(freshServer()),certificateForm=ref(freshCertificate()),locationForm=ref(freshLocation());
const contentTypeOptions=ref<DictionaryOption[]>([]); const methodOptions=ref<DictionaryOption[]>([]);
const selectedCenter=computed(()=>centers.value.find(v=>v.id===selectedCenterId.value)); const selectedServer=computed(()=>servers.value.find(v=>v.id===selectedServerId.value)); const upstreamOptions=computed(()=>upstreams.value.map(v=>({value:v.id,label:v.name}))); const certificateOptions=computed(()=>certificates.value.filter(v=>v.enabled).map(v=>({value:v.id,label:`${v.name}（${v.commonName}）`}))); const upstreamName=(id?:string)=>upstreams.value.find(v=>v.id===id)?.name||'未绑定'; const hasCenter=computed(()=>Boolean(selectedCenterId.value)); const hasServer=computed(()=>Boolean(selectedServerId.value));
const serverColumns=[{dataIndex:'domain',key:'domain',title:'域名'},{dataIndex:'listenPort',key:'listenPort',title:'端口'},{dataIndex:'sslEnabled',key:'sslEnabled',title:'TLS'},{dataIndex:'upstreamId',key:'upstreamId',title:'默认 Upstream'},{key:'action',title:'操作',width:200}]; const upstreamColumns=[{dataIndex:'name',key:'name',title:'服务标识'},{dataIndex:'keepaliveConnections',key:'keepaliveConnections',title:'Keepalive 连接数'},{key:'action',title:'操作',width:130}]; const locationColumns=[{dataIndex:'path',key:'path',title:'匹配路径'},{dataIndex:'methods',key:'methods',title:'请求方法'},{dataIndex:'contentTypes',key:'contentTypes',title:'Content-Type'},{dataIndex:'upstreamId',key:'upstreamId',title:'转发 Upstream'},{key:'condition',title:'长度条件'},{key:'action',title:'操作',width:130}];
async function request<T>(path:string,options?:RequestInit):Promise<T>{const r=await fetch(`/api${path}`,{...options,headers:{'Content-Type':'application/json',...(options?.headers??{})}});if(!r.ok){const b=await r.json().catch(()=>({}));throw new Error(b.detail||b.message||'请求失败');}return r.status===204?(undefined as T):r.json() as Promise<T>}
async function loadCenters(){loading.value=true;try{centers.value=await request<Center[]>('/centers');if(!selectedCenterId.value&&centers.value[0])await selectCenter(centers.value[0].id)}catch(e){message.error(e instanceof Error?e.message:'加载中心失败')}finally{loading.value=false}}
async function loadDictionaries(){try{[contentTypeOptions.value,methodOptions.value]=await Promise.all([request<DictionaryOption[]>('/dictionaries/HTTP_CONTENT_TYPE'),request<DictionaryOption[]>('/dictionaries/HTTP_METHOD')])}catch(e){message.error(e instanceof Error?e.message:'加载下拉选项失败')}}
async function selectCenter(id:string){selectedCenterId.value=id;selectedServerId.value=undefined;locations.value=[];try{[upstreams.value,servers.value,certificates.value]=await Promise.all([request<Upstream[]>(`/centers/${id}/http/upstreams`),request<HttpServer[]>(`/centers/${id}/http/servers`),request<Certificate[]>(`/centers/${id}/tls-certificates`)])}catch(e){message.error(e instanceof Error?e.message:'加载 HTTP 配置失败')}}
async function selectServer(id:string){selectedServerId.value=id;locations.value=[];if(!selectedCenterId.value)return;try{locations.value=await request<HttpLocation[]>(`/centers/${selectedCenterId.value}/http/servers/${id}/locations`)}catch(e){message.error(e instanceof Error?e.message:'加载 Location 失败')}}
function openUpstream(v?:Upstream){editingUpstreamId.value=v?.id;upstreamForm.value=v?{name:v.name,keepaliveConnections:v.keepaliveConnections}:freshUpstream();upstreamOpen.value=true} function openServer(v?:HttpServer){editingServerId.value=v?.id;serverForm.value=v?{domain:v.domain,listenPort:v.listenPort,sslEnabled:v.sslEnabled,upstreamId:v.upstreamId,accessLog:v.accessLog||'',errorLog:v.errorLog||''}:freshServer();serverOpen.value=true} function openLocation(v?:HttpLocation){editingLocationId.value=v?.id;locationForm.value=v?{...v}:freshLocation();locationOpen.value=true}
function openCertificate(v?:Certificate){editingCertificateId.value=v?.id;certificateForm.value=v?{name:v.name,commonName:v.commonName,certificatePem:'',privateKeyPem:'',chainPem:'',enabled:v.enabled}:freshCertificate();certificateOpen.value=true}
async function saveUpstream(){if(!selectedCenterId.value)return;try{await request(`/centers/${selectedCenterId.value}/http/upstreams${editingUpstreamId.value?`/${editingUpstreamId.value}`:''}`,{method:editingUpstreamId.value?'PUT':'POST',body:JSON.stringify(upstreamForm.value)});upstreamOpen.value=false;await selectCenter(selectedCenterId.value);message.success(editingUpstreamId.value?'Upstream 已更新':'Upstream 已创建')}catch(e){message.error(e instanceof Error?e.message:'保存 Upstream 失败')}}
async function saveServer(){if(!selectedCenterId.value)return;try{const result=await request<HttpServer>(`/centers/${selectedCenterId.value}/http/servers${editingServerId.value?`/${editingServerId.value}`:''}`,{method:editingServerId.value?'PUT':'POST',body:JSON.stringify(serverForm.value)});serverOpen.value=false;await selectCenter(selectedCenterId.value);await selectServer(result.id);message.success(editingServerId.value?'HTTP Server 已更新':'HTTP Server 已创建')}catch(e){message.error(e instanceof Error?e.message:'保存 HTTP Server 失败')}}
async function saveCertificate(){if(!selectedCenterId.value)return;try{const payload={...certificateForm.value};if(editingCertificateId.value&&!payload.certificatePem&&!payload.privateKeyPem){delete (payload as any).certificatePem;delete (payload as any).privateKeyPem;}await request(`/centers/${selectedCenterId.value}/tls-certificates${editingCertificateId.value?`/${editingCertificateId.value}`:''}`,{method:editingCertificateId.value?'PUT':'POST',body:JSON.stringify(payload)});certificateOpen.value=false;await selectCenter(selectedCenterId.value);message.success(editingCertificateId.value?'证书已更新':'证书已新增')}catch(e){message.error(e instanceof Error?e.message:'保存证书失败')}}
async function saveLocation(){if(!selectedCenterId.value||!selectedServerId.value||!locationForm.value.upstreamId)return;try{await request(`/centers/${selectedCenterId.value}/http/servers/${selectedServerId.value}/locations${editingLocationId.value?`/${editingLocationId.value}`:''}`,{method:editingLocationId.value?'PUT':'POST',body:JSON.stringify(locationForm.value)});locationOpen.value=false;await selectServer(selectedServerId.value);message.success(editingLocationId.value?'Location 已更新':'Location 已创建')}catch(e){message.error(e instanceof Error?e.message:'保存 Location 失败')}}
async function remove(path:string,title:string){try{await request(path,{method:'DELETE'});if(selectedCenterId.value)await selectCenter(selectedCenterId.value);message.success(`${title}已删除`)}catch(e){message.error(e instanceof Error?e.message:`删除${title}失败`)}}
onMounted(()=>{loadCenters();loadDictionaries()});
</script>
<template>
  <div class="http-config-page p-5">
    <a-card :bordered="false" title="HTTP 配置管理">
      <a-alert class="mb-4" type="info" show-icon message="按 Upstream、Server、Location 的顺序维护配置。选择 Server 后即可管理其路由。" />
      <a-form layout="inline">
        <a-form-item label="配置中心">
          <a-select v-model:value="selectedCenterId" class="w-80" placeholder="请选择要维护的中心" :options="centers.map(v=>({value:v.id,label:`${v.name}（${v.code}）`}))" @change="selectCenter" />
        </a-form-item>
        <span v-if="selectedCenter" class="selection-hint">当前中心：{{ selectedCenter.name }} · 已加载 {{ upstreams.length }} 个 Upstream、{{ servers.length }} 个 Server</span>
      </a-form>
    </a-card>

    <a-row class="mt-5" :gutter="16">
      <a-col :lg="10" :xs="24">
        <a-card :bordered="false" :title="selectedCenter ? `${selectedCenter.name} 的 Upstream` : 'HTTP Upstream'">
          <template #extra><a-button type="primary" :disabled="!hasCenter" @click="openUpstream()">新增 Upstream</a-button></template>
          <a-table :columns="upstreamColumns" :data-source="upstreams" :loading="loading" :pagination="false" row-key="id" size="small" :locale="{ emptyText: hasCenter ? '还没有 Upstream，请先新增一个服务后端。' : '请先选择配置中心。' }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'"><a-space :size="0"><a-button type="link" @click="openUpstream(record)">编辑</a-button><a-popconfirm title="确认删除该 Upstream？" @confirm="remove(`/centers/${selectedCenterId}/http/upstreams/${record.id}`, 'Upstream')"><a-button danger type="link">删除</a-button></a-popconfirm></a-space></template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :lg="14" :xs="24" class="max-lg:mt-4">
        <a-card :bordered="false" :title="selectedCenter ? `${selectedCenter.name} 的 Server` : 'HTTP Server'">
          <template #extra><a-button type="primary" :disabled="!hasCenter" @click="openServer()">新增 Server</a-button></template>
          <a-table :columns="serverColumns" :data-source="servers" :loading="loading" :pagination="false" row-key="id" size="small" :row-class-name="(record: HttpServer) => record.id === selectedServerId ? 'selected-server-row' : ''" :locale="{ emptyText: hasCenter ? '还没有 Server，请新增域名和监听端口。' : '请先选择配置中心。' }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'sslEnabled'"><a-tag :color="record.sslEnabled ? 'green' : 'default'">{{ record.sslEnabled ? '已启用' : '未启用' }}</a-tag></template>
              <template v-else-if="column.key === 'upstreamId'">{{ upstreamName(record.upstreamId) }}</template>
              <template v-else-if="column.key === 'action'"><a-space :size="0"><a-button type="link" :type="record.id === selectedServerId ? 'primary' : 'link'" @click="selectServer(record.id)">{{ record.id === selectedServerId ? '正在管理' : '管理 Location' }}</a-button><a-button type="link" @click="openServer(record)">编辑</a-button><a-popconfirm title="确认删除该 Server 及其全部 Location？" @confirm="remove(`/centers/${selectedCenterId}/http/servers/${record.id}`, 'Server')"><a-button danger type="link">删除</a-button></a-popconfirm></a-space></template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="mt-5" :bordered="false" :title="selectedServer ? `${selectedServer.domain}:${selectedServer.listenPort} 的 Location` : 'Location 路由'">
      <template #extra><a-button type="primary" :disabled="!hasServer" @click="openLocation()">新增 Location</a-button></template>
      <template v-if="!hasServer"><a-empty description="从上方选择一个 Server 后，可在此维护 Location 路由。" /></template>
      <a-table v-else :columns="locationColumns" :data-source="locations" :pagination="false" row-key="id" :locale="{ emptyText: '该 Server 暂无 Location，新增一条路由开始配置。' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'methods'"><a-tag v-for="item in record.methods" :key="item">{{ item }}</a-tag></template>
          <template v-else-if="column.key === 'contentTypes'"><span :class="{ 'text-gray-400': !record.contentTypes.length }">{{ record.contentTypes.length ? record.contentTypes.join(', ') : '不限制' }}</span></template>
          <template v-else-if="column.key === 'upstreamId'">{{ upstreamName(record.upstreamId) }}</template>
          <template v-else-if="column.key === 'condition'">请求头 {{ record.headerLengthMin }} ～ {{ record.headerLengthMax }}；请求体 {{ record.bodyLengthMin }} ～ {{ record.bodyLengthMax }}</template>
          <template v-else-if="column.key === 'action'"><a-space :size="0"><a-button type="link" @click="openLocation(record)">编辑</a-button><a-popconfirm title="确认删除该 Location？" @confirm="remove(`/centers/${selectedCenterId}/http/servers/${selectedServerId}/locations/${record.id}`, 'Location')"><a-button danger type="link">删除</a-button></a-popconfirm></a-space></template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="upstreamOpen" :title="editingUpstreamId ? '编辑 HTTP Upstream' : '新增 HTTP Upstream'" :width="480" destroy-on-close>
      <a-alert class="mb-4" type="info" show-icon message="Upstream 用于定义可被 Server 和 Location 引用的后端服务。" />
      <a-form layout="vertical"><a-form-item label="服务标识" required><a-input v-model:value="upstreamForm.name" placeholder="例如：order-service" /></a-form-item><a-form-item label="Keepalive 连接数" required extra="空闲连接池的最大连接数。"><a-input-number v-model:value="upstreamForm.keepaliveConnections" class="w-full" :min="1" :max="10000" /></a-form-item></a-form>
      <template #footer><div class="flex justify-end gap-2"><a-button @click="upstreamOpen = false">取消</a-button><a-button type="primary" @click="saveUpstream">保存</a-button></div></template>
    </a-drawer>

    <a-drawer v-model:open="serverOpen" :title="editingServerId ? '编辑 HTTP Server' : '新增 HTTP Server'" :width="520" destroy-on-close>
      <a-alert class="mb-4" type="info" show-icon message="一个 Server 对应一个域名和监听端口；路由在保存后通过“管理 Location”维护。" />
      <a-form layout="vertical"><a-row :gutter="16"><a-col :span="14"><a-form-item label="域名" required><a-input v-model:value="serverForm.domain" placeholder="例如：api.example.com" /></a-form-item></a-col><a-col :span="10"><a-form-item label="监听端口" required><a-input-number v-model:value="serverForm.listenPort" class="w-full" :min="1" :max="65535" /></a-form-item></a-col></a-row><a-form-item label="默认 Upstream" extra="当 Location 未单独指定时使用；可稍后在 Location 中覆盖。"><a-select v-model:value="serverForm.upstreamId" allow-clear show-search placeholder="请选择后端服务" :options="upstreamOptions" /></a-form-item><a-row :gutter="16"><a-col :span="12"><a-form-item label="访问日志"><a-input v-model:value="serverForm.accessLog" placeholder="留空自动生成" /></a-form-item></a-col><a-col :span="12"><a-form-item label="错误日志"><a-input v-model:value="serverForm.errorLog" placeholder="留空自动生成" /></a-form-item></a-col></a-row><a-form-item><a-checkbox v-model:checked="serverForm.sslEnabled">启用 TLS</a-checkbox></a-form-item></a-form>
      <template #footer><div class="flex justify-end gap-2"><a-button @click="serverOpen = false">取消</a-button><a-button type="primary" @click="saveServer">保存</a-button></div></template>
    </a-drawer>

    <a-drawer v-model:open="locationOpen" :title="editingLocationId ? '编辑 HTTP Location' : '新增 HTTP Location'" :width="720" destroy-on-close>
      <a-alert class="mb-4" type="info" show-icon message="Location 的匹配条件会同时参与运行时访问策略判断；Content-Type 留空时不限制类型。" />
      <a-form layout="vertical"><a-row :gutter="16"><a-col :span="12"><a-form-item label="匹配路径" required><a-input v-model:value="locationForm.path" placeholder="例如：/orders" /></a-form-item></a-col><a-col :span="12"><a-form-item label="转发 Upstream" required><a-select v-model:value="locationForm.upstreamId" show-search placeholder="请选择后端服务" :options="upstreamOptions" /></a-form-item></a-col></a-row><a-form-item label="允许的请求方法" required extra="可选择多个请求方法。"><a-select v-model:value="locationForm.methods" mode="multiple" :options="methodOptions" option-filter-prop="label" placeholder="请选择请求方法" /></a-form-item><a-form-item label="允许的 Content-Type" extra="推荐项由系统字典提供；也可直接输入自定义类型后按回车添加。"><a-select v-model:value="locationForm.contentTypes" mode="tags" :options="contentTypeOptions" option-filter-prop="label" :token-separators="[',']" placeholder="不填写表示不限制" /></a-form-item><a-divider orientation="left">请求大小条件（字节）</a-divider><a-row :gutter="16"><a-col :span="6"><a-form-item label="请求头最小"><a-input-number v-model:value="locationForm.headerLengthMin" class="w-full" :min="0" /></a-form-item></a-col><a-col :span="6"><a-form-item label="请求头最大"><a-input-number v-model:value="locationForm.headerLengthMax" class="w-full" :min="0" /></a-form-item></a-col><a-col :span="6"><a-form-item label="请求体最小"><a-input-number v-model:value="locationForm.bodyLengthMin" class="w-full" :min="0" /></a-form-item></a-col><a-col :span="6"><a-form-item label="请求体最大"><a-input-number v-model:value="locationForm.bodyLengthMax" class="w-full" :min="0" /></a-form-item></a-col></a-row><a-divider orientation="left">代理超时（毫秒）</a-divider><a-row :gutter="16"><a-col :span="8"><a-form-item label="连接"><a-input-number v-model:value="locationForm.proxyConnectTimeoutMs" class="w-full" :min="1" /></a-form-item></a-col><a-col :span="8"><a-form-item label="读取"><a-input-number v-model:value="locationForm.proxyReadTimeoutMs" class="w-full" :min="1" /></a-form-item></a-col><a-col :span="8"><a-form-item label="发送"><a-input-number v-model:value="locationForm.proxySendTimeoutMs" class="w-full" :min="1" /></a-form-item></a-col></a-row></a-form>
      <template #footer><div class="flex justify-end gap-2"><a-button @click="locationOpen = false">取消</a-button><a-button type="primary" @click="saveLocation">保存</a-button></div></template>
    </a-drawer>
  </div>
</template>
<style scoped>
.selection-hint { color: var(--ant-color-text-description); font-size: 13px; line-height: 32px; }
:deep(.selected-server-row > td) { background: var(--ant-color-primary-bg); }
</style>
