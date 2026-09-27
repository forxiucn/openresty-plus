<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { Button as AButton, Card as ACard, Form as AForm, FormItem as AFormItem, Select as ASelect, Space as ASpace, Table as ATable, Tag as ATag, notification, Popconfirm } from 'ant-design-vue';
import ServerEditorDrawer from '#/components/openresty/ServerEditorDrawer.vue';
import { type PageResult, useServerPagination } from '#/utils/server-pagination';
type Center={id:string;name:string;code:string}; type Server={id:string;domain:string;listenPort:number;sslEnabled:boolean;certificateId?:string;upstreamId?:string;accessLog?:string;errorLog?:string;rootPath?:string;responseHeaders:string[]};
const centers=ref<Center[]>([]), rows=ref<Server[]>([]), centerId=ref<string>();
const drawerOpen=ref(false), editingServer=ref<Server>();
const upstreams=ref<Array<{id:string;name:string}>>([]);
const pager=useServerPagination();
const columns=[{dataIndex:'domain',title:'域名'},{dataIndex:'listenPort',title:'端口'},{key:'tls',title:'TLS'},{key:'action',title:'操作'}];
async function api<T>(path:string, options?:RequestInit):Promise<T>{const response=await fetch(`/api${path}`,{...options,headers:{'Content-Type':'application/json',...(options?.headers||{})}});if(!response.ok)throw new Error(await response.text()||`请求失败（${response.status}）`);return response.status===204?(undefined as T):response.json()};
async function load(){if(centerId.value){rows.value=pager.apply(await api<PageResult<Server>>(`/centers/${centerId.value}/http/servers/paged?${pager.query()}`));upstreams.value=await api(`/centers/${centerId.value}/http/upstreams`)}};
function open(value:Server){openEditor(value)}
function openEditor(value?:Server){editingServer.value=value;drawerOpen.value=true}
async function remove(value:Server){try{await api(`/centers/${centerId.value}/http/servers/${value.id}`,{method:'DELETE'});notification.success({message:'Server 已删除',placement:'topRight'});await load()}catch(error){notification.error({message:'删除 Server 失败',description:error instanceof Error?error.message:'请稍后重试',placement:'topRight'})}}
onMounted(async()=>{centers.value=await api<Center[]>('/centers');centerId.value=centers.value[0]?.id;await load()});
</script>
<template><div class="ops-page"><a-card :bordered="false" title="HTTP Server 配置"><a-form layout="inline"><a-form-item label="中心"><a-select v-model:value="centerId" class="w-72" :options="centers.map(v=>({value:v.id,label:`${v.name}（${v.code}）`}))" @change="load"/></a-form-item><a-form-item><a-button type="primary" @click="openEditor()">新增 Server</a-button></a-form-item></a-form></a-card><a-card class="mt-4" :bordered="false"><a-table :columns="columns" :data-source="rows" :pagination="pager.table.value" row-key="id" @change="(p:any)=>{pager.change(p);load()}"><template #bodyCell="{column,record}"><template v-if="column.key==='tls'"><a-tag :color="record.sslEnabled?'green':'default'">{{record.sslEnabled?'已启用':'未启用'}}</a-tag></template><template v-else-if="column.key==='action'"><a-space><a-button type="link" @click="open(record)">编辑配置</a-button><a-popconfirm title="确定删除该 Server 及其 Location 配置吗？" @confirm="remove(record)"><a-button type="link" danger>删除</a-button></a-popconfirm></a-space></template></template></a-table></a-card><server-editor-drawer v-model:open="drawerOpen" :center-id="centerId" :server="editingServer" :upstreams="upstreams" @saved="load"/></div></template>
