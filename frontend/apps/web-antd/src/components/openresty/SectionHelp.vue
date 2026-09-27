<script lang="ts" setup>
import { QuestionCircleOutlined } from '@ant-design/icons-vue';
import { Tooltip as ATooltip } from 'ant-design-vue';

defineProps<{ text: string }>();
</script>

<template>
  <a-tooltip :title="text">
    <span class="section-help"><QuestionCircleOutlined /> 配置说明</span>
  </a-tooltip>
</template>

<style scoped>
.section-help { display: inline-flex; align-items: center; gap: 6px; color: var(--ant-color-text-secondary); font-size: 13px; font-weight: 400; cursor: help; }
.section-help :deep(svg) { color: var(--ant-color-primary); font-size: 15px; }
</style>
