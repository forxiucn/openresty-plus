<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import { nextTick, onMounted, ref, watch } from 'vue';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

type ChartItem = { name: string; value: number };
const props = withDefaults(defineProps<{ data: ChartItem[]; kind?: 'bar' | 'donut'; color?: string[] }>(), { kind: 'bar' });
const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

async function render() {
  await nextTick();
  const palette = props.color || ['#1677ff', '#13c2c2', '#52c41a', '#faad14', '#722ed1', '#ff4d4f'];
  if (props.kind === 'donut') {
    renderEcharts({
      color: palette,
      legend: { bottom: 0, icon: 'circle', itemGap: 18 },
      series: [{ center: ['50%', '43%'], data: props.data.map((item, index) => ({ ...item, itemStyle: { color: palette[index % palette.length] } })), radius: ['48%', '72%'], type: 'pie', label: { formatter: '{b}\n{c}' } }],
      tooltip: { trigger: 'item', formatter: '{b}：{c}（{d}%）' },
    });
    return;
  }
  renderEcharts({
    color: palette,
    grid: { bottom: 12, containLabel: true, left: 12, right: 18, top: 20 },
    series: [{ barMaxWidth: 42, data: props.data.map((item, index) => ({ value: item.value, itemStyle: { color: palette[index % palette.length] } })), itemStyle: { borderRadius: [6, 6, 0, 0] }, type: 'bar' }],
    tooltip: { trigger: 'axis' },
    xAxis: { axisLabel: { interval: 0 }, data: props.data.map((item) => item.name), type: 'category' },
    yAxis: { minInterval: 1, splitLine: { lineStyle: { type: 'dashed' } }, type: 'value' },
  });
}

onMounted(render);
watch(() => props.data, render, { deep: true });
</script>

<template><EchartsUI ref="chartRef" /></template>
