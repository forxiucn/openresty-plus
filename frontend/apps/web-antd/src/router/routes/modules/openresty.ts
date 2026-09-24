import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    component: () => import('#/views/openresty/centers/index.vue'),
    meta: {
      affixTab: true,
      icon: 'lucide:map-pinned',
      order: -10,
      title: '中心与节点',
    },
    name: 'CenterManagement',
    path: '/openresty/centers',
  },
  {
    component: () => import('#/views/openresty/http-config/index.vue'),
    meta: {
      icon: 'lucide:globe-2',
      order: -9,
      title: 'HTTP 配置',
    },
    name: 'HttpConfigurationManagement',
    path: '/openresty/http-config',
  },
  {
    component: () => import('#/views/openresty/upstreams/index.vue'),
    meta: {
      icon: 'lucide:route',
      order: -8.5,
      title: 'Upstream 配置',
    },
    name: 'UpstreamConfigurationManagement',
    path: '/openresty/upstreams',
  },
  {
    component: () => import('#/views/openresty/runtime-configurations/index.vue'),
    meta: {
      icon: 'lucide:history',
      order: -8,
      title: '版本与审计',
    },
    name: 'RuntimeConfigurationManagement',
    path: '/openresty/runtime-configurations',
  },
  {
    component: () => import('#/views/openresty/dns-resolvers/index.vue'),
    meta: { icon: 'lucide:server-cog', order: -7.5, title: 'DNS Resolver' },
    name: 'DnsResolverManagement',
    path: '/openresty/dns-resolvers',
  },
  {
    component: () => import('#/views/openresty/stream-config/index.vue'),
    meta: {
      icon: 'lucide:network',
      order: -7,
      title: 'Stream 配置',
    },
    name: 'StreamConfigurationManagement',
    path: '/openresty/stream-config',
  },
  {
    component: () => import('#/views/openresty/policies/index.vue'),
    meta: {
      icon: 'lucide:shield-check',
      order: -6,
      title: '访问策略',
    },
    name: 'AccessPolicyManagement',
    path: '/openresty/policies',
  },
];

export default routes;
