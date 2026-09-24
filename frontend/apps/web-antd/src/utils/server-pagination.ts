import { computed, ref } from 'vue';

export type PageResult<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
};

export function useServerPagination(defaultSize = 10) {
  const page = ref(0);
  const size = ref(defaultSize);
  const total = ref(0);
  const table = computed(() => ({
    current: page.value + 1,
    pageSize: size.value,
    total: total.value,
    showSizeChanger: true,
    showTotal: (value: number) => `共 ${value} 条`,
  }));
  function apply<T>(result: PageResult<T>) {
    page.value = result.page;
    size.value = result.size;
    total.value = result.total;
    return result.items;
  }
  function change(value: { current?: number; pageSize?: number }) {
    const nextSize = value.pageSize ?? size.value;
    page.value = nextSize === size.value ? Math.max((value.current ?? 1) - 1, 0) : 0;
    size.value = nextSize;
  }
  function query() { return `page=${page.value}&size=${size.value}`; }
  function reset() { page.value = 0; }
  return { page, size, total, table, apply, change, query, reset };
}
