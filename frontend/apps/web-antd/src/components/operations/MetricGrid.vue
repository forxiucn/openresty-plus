<script lang="ts" setup>
type Metric = {
  label: string;
  value: number | string;
  suffix?: string;
  hint?: string;
  tone?: 'blue' | 'cyan' | 'green' | 'orange' | 'purple' | 'red';
};

withDefaults(defineProps<{ metrics: Metric[]; columns?: number }>(), { columns: 4 });
</script>

<template>
  <div class="ops-metric-grid" :style="{ '--metric-columns': columns }">
    <div v-for="metric in metrics" :key="metric.label" class="ops-metric" :data-tone="metric.tone || 'blue'">
      <div class="ops-metric__label">{{ metric.label }}</div>
      <div class="ops-metric__value">{{ metric.value }}<span v-if="metric.suffix">{{ metric.suffix }}</span></div>
      <div v-if="metric.hint" class="ops-metric__hint">{{ metric.hint }}</div>
    </div>
  </div>
</template>

