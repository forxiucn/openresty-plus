<script lang="ts" setup>
import { computed } from 'vue';

import { Button as AButton, Input as AInput, Select as ASelect } from 'ant-design-vue';

type KeyValue = { key: string; value: string };

const props = withDefaults(defineProps<{
  modelValue?: string[];
  nameOptions?: string[];
  namePlaceholder?: string;
  valuePlaceholder?: string;
}>(), {
  modelValue: () => [],
  nameOptions: () => [],
  namePlaceholder: 'Header 名称，可自定义',
  valuePlaceholder: 'Header 值',
});

const emit = defineEmits<{ 'update:modelValue': [value: string[]] }>();

const rows = computed<KeyValue[]>(() => props.modelValue.length
  ? props.modelValue.map((item) => {
      const index = item.indexOf(':');
      return index < 0 ? { key: item.trim(), value: '' } : { key: item.slice(0, index).trim(), value: item.slice(index + 1).trim() };
    })
  : [{ key: '', value: '' }]);

const options = computed(() => props.nameOptions.map((value) => ({ label: value, value })));

function update(index: number, field: keyof KeyValue, value: string) {
  const next = rows.value.map((item) => ({ ...item }));
  next[index][field] = value;
  emit('update:modelValue', next.filter((item) => item.key.trim()).map((item) => `${item.key.trim()}: ${item.value}`));
}

function add() {
  emit('update:modelValue', [...props.modelValue, ': ']);
}

function remove(index: number) {
  const next = [...props.modelValue];
  next.splice(index, 1);
  emit('update:modelValue', next);
}
</script>

<template>
  <div class="key-value-editor">
    <a-space-compact v-for="(row, index) in rows" :key="`${index}-${row.key}`" block class="key-value-row">
      <a-select
        :value="row.key"
        allow-clear
        mode="combobox"
        show-search
        :options="options"
        :placeholder="namePlaceholder"
        @update:value="(value) => update(index, 'key', String(value || ''))"
      />
      <a-input :value="row.value" :placeholder="valuePlaceholder" @update:value="(value) => update(index, 'value', value)" />
      <a-button danger @click="remove(index)">删除</a-button>
    </a-space-compact>
    <a-button class="mt-2" @click="add">新增一行</a-button>
  </div>
</template>

<style scoped>
.key-value-editor { display: grid; gap: 8px; }
.key-value-row { display: grid !important; grid-template-columns: minmax(220px, 32%) minmax(0, 1fr) 68px; width: 100%; }
.key-value-row :deep(.ant-select), .key-value-row :deep(.ant-input) { width: 100%; min-width: 0; }
@media (max-width: 640px) { .key-value-row { grid-template-columns: 1fr 56px; } .key-value-row :deep(.ant-select) { grid-column: 1 / -1; } }
</style>
