<template>
  <div class="app-container">
    <div class="search-container">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="佣金单号">
          <el-input
            v-model="queryParams.commissionNo"
            placeholder="佣金单号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="主持人">
          <el-input
            v-model="queryParams.hostName"
            placeholder="主持人姓名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="佣金状态" style="width: 150px;">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="待支付" :value="1" />
            <el-option label="已支付" :value="2" />
            <el-option label="逾期" :value="3" />
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
        <el-table-column label="佣金单号" prop="commissionNo" width="180" />
        <el-table-column label="订单号" prop="orderNo" width="180" />
        <el-table-column label="主持人" prop="hostName" width="120" />
        <el-table-column label="订单金额" width="120">
          <template #default="scope">
            ¥{{ Number(scope.row.orderAmount).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="佣金比例" width="100">
          <template #default="scope">
            {{ scope.row.commissionRate }}%
          </template>
        </el-table-column>
        <el-table-column label="佣金金额" width="120">
          <template #default="scope">
            ¥{{ Number(scope.row.commissionAmount).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止日期" prop="deadline" width="170" />
        <el-table-column label="支付时间" prop="payTime" width="170" />
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" fixed="right" width="260">
          <template #default="scope">
            <el-button type="primary" icon="view" link size="small" @click="handleView(scope.row.id)">
              查看
            </el-button>
            <el-button
              v-if="scope.row.status === 1 && isPastDeadline(scope.row.deadline)"
              type="warning"
              link
              size="small"
              @click="handleMarkOverdue(scope.row.id)"
            >
              标记逾期
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="handleBanHost(scope.row.hostId)"
            >
              禁止接单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-if="total > 0"
        v-model:total="total"
        v-model:page="queryParams.current"
        v-model:limit="queryParams.size"
        @pagination="fetchData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="佣金单号">{{ detail.commissionNo }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="主持人">{{ detail.hostName }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥{{ Number(detail.orderAmount).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="佣金比例">{{ detail.commissionRate }}%</el-descriptions-item>
        <el-descriptions-item label="佣金金额">¥{{ Number(detail.commissionAmount).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detail.status)">
            {{ getStatusText(detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="截止日期">{{ detail.deadline || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="dialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import { getCommissionPage, getCommissionById, markCommissionOverdue, banHost } from '@/api/marrylink-api'

const queryParams = reactive({
  current: 1,
  size: 10,
  commissionNo: '',
  hostName: '',
  status: null
})

const pageData = ref([])
const total = ref(0)
const loading = ref(false)

const dialog = reactive({
  visible: false,
  title: '佣金详情'
})

const detail = reactive({
  commissionNo: '',
  orderNo: '',
  hostName: '',
  orderAmount: 0,
  commissionRate: 0,
  commissionAmount: 0,
  status: null,
  deadline: '',
  payTime: '',
  createTime: ''
})

function getStatusType(status) {
  const types = { 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { 1: '待支付', 2: '已支付', 3: '逾期' }
  return texts[status] || '未知'
}

function isPastDeadline(deadline) {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getCommissionPage(queryParams)
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
  queryParams.commissionNo = ''
  queryParams.hostName = ''
  queryParams.status = null
  handleQuery()
}

async function handleView(id) {
  const res = await getCommissionById(id)
  Object.assign(detail, res)
  dialog.title = '佣金详情'
  dialog.visible = true
}

function handleMarkOverdue(id) {
  ElMessageBox.confirm('确认将该佣金标记为逾期?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await markCommissionOverdue(id)
    ElMessage.success('已标记为逾期')
    fetchData()
  })
}

function handleBanHost(hostId) {
  ElMessageBox.confirm('确认禁止该主持人接单?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await banHost(hostId)
    ElMessage.success('已禁止接单')
    fetchData()
  })
}

onMounted(() => {
  fetchData()
})
</script>
