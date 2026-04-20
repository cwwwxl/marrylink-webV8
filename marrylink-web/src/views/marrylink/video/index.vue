<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="视频标题"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="状态" style="width: 150px;">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="待审核" :value="2" />
            <el-option label="已禁用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="首页展示" style="width: 170px;">
          <el-select v-model="queryParams.showOnHome" placeholder="全部" clearable>
            <el-option label="首页展示" :value="1" />
            <el-option label="不展示" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
          <el-button icon="refresh" @click="handleResetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="pageData" border stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="视频封面" width="120">
          <template #default="scope">
            <el-image
              v-if="scope.row.coverUrl"
              :src="scope.row.coverUrl"
              :preview-src-list="[scope.row.coverUrl]"
              fit="cover"
              style="width: 80px; height: 60px; border-radius: 4px;"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="150" show-overflow-tooltip />
        <el-table-column label="主持人" prop="hostId" width="100" />
        <el-table-column label="文件大小" width="100">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首页展示" width="100">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.showOnHome === 1"
              @change="(val) => handleToggleShowOnHome(scope.row.id, val ? 1 : 0)"
            />
          </template>
        </el-table-column>
        <el-table-column label="上传时间" prop="createTime" width="170" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="scope">
            <el-button type="primary" icon="edit" link size="small" @click="handleOpenDialog(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" icon="delete" link size="small" @click="handleDelete(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-if="total > 0"
        :total="total"
        :page="queryParams.current"
        :limit="queryParams.size"
        @update:page="queryParams.current = $event"
        @update:limit="queryParams.size = $event"
        @pagination="fetchData"
      />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="500px" @close="handleCloseDialog">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入视频标题" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入视频描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCloseDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getHostVideoPage,
  updateHostVideo,
  deleteHostVideo,
  updateHostVideoShowOnHome
} from '@/api/marrylink-api'
import Pagination from '@/components/Pagination/index.vue'

const queryParams = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: undefined,
  showOnHome: undefined
})

const pageData = ref([])
const total = ref(0)
const loading = ref(false)

const dialog = reactive({
  visible: false,
  title: '编辑视频'
})

const formData = reactive({
  id: null,
  title: '',
  description: ''
})

const formRef = ref()

const rules = {
  title: [{ required: true, message: '请输入视频标题', trigger: 'blur' }]
}

function statusTagType(status) {
  if (status === 1) return 'success'
  if (status === 2) return 'warning'
  return 'danger'
}

function statusLabel(status) {
  if (status === 1) return '正常'
  if (status === 2) return '待审核'
  return '已禁用'
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getHostVideoPage(queryParams)
    pageData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.current = 1
  fetchData()
}

function handleResetQuery() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.showOnHome = undefined
  handleQuery()
}

function handleOpenDialog(row) {
  dialog.visible = true
  dialog.title = '编辑视频'
  formData.id = row.id
  formData.title = row.title || ''
  formData.description = row.description || ''
}

function handleCloseDialog() {
  dialog.visible = false
  formRef.value?.resetFields()
  Object.assign(formData, { id: null, title: '', description: '' })
}

function handleSubmit() {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await updateHostVideo(formData)
        ElMessage.success('修改成功')
        handleCloseDialog()
        fetchData()
      } finally {
        loading.value = false
      }
    }
  })
}

function handleDelete(id) {
  ElMessageBox.confirm('确认删除该视频?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteHostVideo(id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

async function handleToggleShowOnHome(id, val) {
  try {
    await updateHostVideoShowOnHome(id, val)
    ElMessage.success('更新成功')
    fetchData()
  } catch {
    ElMessage.error('更新失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
:deep(.el-table__cell) {
  line-height: 1.8;
}
</style>
