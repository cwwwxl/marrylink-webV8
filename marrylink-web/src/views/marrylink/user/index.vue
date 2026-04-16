<template>
  <div class="app-container">
    <div class="search-container">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="新人姓名/手机号/用户编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="状态" style="width: 150px;">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
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
        <el-table-column label="新人信息" width="200">
          <template #default="scope">
            <div>{{ scope.row.brideName }} & {{ scope.row.groomName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" width="120" />
        <el-table-column label="婚礼类型" prop="weddingType" width="120" />
        <el-table-column label="订单数" prop="orderCount" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status == 1 ? 'success' : 'info'">
              {{ scope.row.status == 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" prop="createTime" width="150" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="scope">
            <el-button type="primary" icon="view" link size="small" @click="handleView(scope.row)">
              查看
            </el-button>
            <el-button 
              :type="scope.row.status == 1 ? 'warning' : 'success'" 
              link 
              size="small" 
              @click="handleToggleStatus(scope.row)"
            >
              {{ scope.row.status == 1 ? '禁用' : '启用' }}
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

    <!-- 查看详情弹窗 -->
    <el-dialog v-model="dialog.visible" title="用户详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="手机号">{{ viewData.phone }}</el-descriptions-item>
        <el-descriptions-item label="新娘姓名">{{ viewData.brideName }}</el-descriptions-item>
        <el-descriptions-item label="新郎姓名">{{ viewData.groomName }}</el-descriptions-item>
        <el-descriptions-item label="婚礼类型">{{ viewData.weddingType }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ viewData.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.status == 1 ? 'success' : 'info'">
            {{ viewData.status == 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ viewData.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, updateUserStatus } from '@/api/marrylink-api'
import Pagination from '@/components/Pagination/index.vue'


const queryParams = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: null
})

const pageData = ref([])
const total = ref(0)
const loading = ref(false)

const dialog = reactive({
  visible: false
})

const viewData = ref({})

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserPage(queryParams)
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
  queryParams.status = null
  handleQuery()
}

function handleView(row) {
  viewData.value = row
  dialog.visible = true
}

function handleToggleStatus(row) {
  const newStatus = row.status == 1 ? 0 : 1
  const action = newStatus == 1 ? '启用' : '禁用'
  
  ElMessageBox.confirm(`确认${action}该用户?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await updateUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchData()
  })
}

onMounted(() => {
  fetchData()
})
</script>