<template>
  <div v-show="!hidden" class="pagination-container">
    <el-pagination
      :current-page="page"
      :page-size="limit"
      :page-sizes="pageSizes"
      :total="total"
      :layout="layout"
      :background="background"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  total: number
  page?: number
  limit?: number
  pageSizes?: number[]
  layout?: string
  background?: boolean
  autoScroll?: boolean
  hidden?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  page: 1,
  limit: 10,
  pageSizes: () => [10, 20, 50, 100],
  layout: 'total, sizes, prev, pager, next, jumper',
  background: true,
  autoScroll: true,
  hidden: false
})

const emit = defineEmits<{
  'update:page': [page: number]
  'update:limit': [limit: number]
  'pagination': [params: { page: number; limit: number }]
}>()

const handleSizeChange = (val: number) => {
  emit('update:limit', val)
  emit('pagination', { page: props.page, limit: val })
  if (props.autoScroll) {
    scrollToTop()
  }
}

const handleCurrentChange = (val: number) => {
  emit('update:page', val)
  emit('pagination', { page: val, limit: props.limit })
  if (props.autoScroll) {
    scrollToTop()
  }
}

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}
</script>

<style scoped>
.pagination-container {
  margin-top: 20px;
  text-align: right;
}
</style>