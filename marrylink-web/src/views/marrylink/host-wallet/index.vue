<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- Tab 1: 钱包列表 -->
      <el-tab-pane label="钱包列表" name="wallet">
        <el-card shadow="hover">
          <el-table v-loading="walletLoading" :data="walletPageData" border stripe>
            <el-table-column label="主持人ID" prop="hostId" width="100" />
            <el-table-column label="主持人姓名" prop="hostName" width="120" />
            <el-table-column label="余额" width="120">
              <template #default="scope">
                ¥{{ Number(scope.row.balance).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="冻结金额" width="120">
              <template #default="scope">
                ¥{{ Number(scope.row.frozenAmount).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="总收入" width="120">
              <template #default="scope">
                ¥{{ Number(scope.row.totalIncome).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="总佣金" width="120">
              <template #default="scope">
                ¥{{ Number(scope.row.totalCommission).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="已提现" width="120">
              <template #default="scope">
                ¥{{ Number(scope.row.totalWithdrawn).toFixed(2) }}
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-if="walletTotal > 0"
            v-model:total="walletTotal"
            v-model:page="walletQueryParams.current"
            v-model:limit="walletQueryParams.size"
            @pagination="fetchWalletData"
          />
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: 提现管理 -->
      <el-tab-pane label="提现管理" name="withdrawal">
        <div class="search-container">
          <el-form :model="withdrawalQueryParams" :inline="true">
            <el-form-item label="提现单号">
              <el-input
                v-model="withdrawalQueryParams.withdrawalNo"
                placeholder="提现单号"
                clearable
                @keyup.enter="handleWithdrawalQuery"
              />
            </el-form-item>

            <el-form-item label="主持人">
              <el-input
                v-model="withdrawalQueryParams.hostName"
                placeholder="主持人姓名"
                clearable
                @keyup.enter="handleWithdrawalQuery"
              />
            </el-form-item>

            <el-form-item label="提现状态" style="width: 150px;">
              <el-select v-model="withdrawalQueryParams.status" placeholder="全部" clearable>
                <el-option label="待审核" :value="1" />
                <el-option label="已通过" :value="2" />
                <el-option label="已拒绝" :value="3" />
                <el-option label="已打款" :value="4" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" icon="search" @click="handleWithdrawalQuery">搜索</el-button>
              <el-button icon="refresh" @click="handleResetWithdrawalQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-card shadow="hover">
          <el-table v-loading="withdrawalLoading" :data="withdrawalPageData" border stripe>
            <el-table-column label="提现单号" prop="withdrawalNo" width="180" />
            <el-table-column label="主持人" prop="hostName" width="120" />
            <el-table-column label="提现金额" width="120">
              <template #default="scope">
                ¥{{ Number(scope.row.amount).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="账户类型" prop="accountType" width="100" />
            <el-table-column label="账户号" prop="accountNo" width="180" />
            <el-table-column label="账户名" prop="accountName" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getWithdrawalStatusType(scope.row.status)">
                  {{ getWithdrawalStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" prop="createTime" width="170" />
            <el-table-column label="审核时间" prop="auditTime" width="170" />
            <el-table-column label="打款时间" prop="payTime" width="170" />
            <el-table-column label="操作" fixed="right" width="220">
              <template #default="scope">
                <template v-if="scope.row.status === 1">
                  <el-button type="success" link size="small" @click="handleApprove(scope.row.id)">
                    通过
                  </el-button>
                  <el-button type="danger" link size="small" @click="handleReject(scope.row.id)">
                    拒绝
                  </el-button>
                </template>
                <el-button
                  v-if="scope.row.status === 2"
                  type="primary"
                  link
                  size="small"
                  @click="handleConfirmPay(scope.row.id)"
                >
                  确认打款
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-if="withdrawalTotal > 0"
            v-model:total="withdrawalTotal"
            v-model:page="withdrawalQueryParams.current"
            v-model:limit="withdrawalQueryParams.size"
            @pagination="fetchWithdrawalData"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectDialog.visible" title="拒绝提现" width="500px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="100px">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import { getHostWalletPage, getWithdrawalPage, auditWithdrawal } from '@/api/marrylink-api'

const activeTab = ref('wallet')

// ==================== 钱包列表 ====================
const walletQueryParams = reactive({
  current: 1,
  size: 10
})
const walletPageData = ref([])
const walletTotal = ref(0)
const walletLoading = ref(false)

async function fetchWalletData() {
  walletLoading.value = true
  try {
    const res = await getHostWalletPage(walletQueryParams)
    walletPageData.value = res.records
    walletTotal.value = res.total
  } finally {
    walletLoading.value = false
  }
}

// ==================== 提现管理 ====================
const withdrawalQueryParams = reactive({
  current: 1,
  size: 10,
  withdrawalNo: '',
  hostName: '',
  status: null
})
const withdrawalPageData = ref([])
const withdrawalTotal = ref(0)
const withdrawalLoading = ref(false)

async function fetchWithdrawalData() {
  withdrawalLoading.value = true
  try {
    const res = await getWithdrawalPage(withdrawalQueryParams)
    withdrawalPageData.value = res.records
    withdrawalTotal.value = res.total
  } finally {
    withdrawalLoading.value = false
  }
}

function handleWithdrawalQuery() {
  withdrawalQueryParams.current = 1
  fetchWithdrawalData()
}

function handleResetWithdrawalQuery() {
  withdrawalQueryParams.withdrawalNo = ''
  withdrawalQueryParams.hostName = ''
  withdrawalQueryParams.status = null
  handleWithdrawalQuery()
}

function getWithdrawalStatusType(status) {
  const types = { 1: 'warning', 2: 'success', 3: 'danger', 4: 'primary' }
  return types[status] || 'info'
}

function getWithdrawalStatusText(status) {
  const texts = { 1: '待审核', 2: '已通过', 3: '已拒绝', 4: '已打款' }
  return texts[status] || '未知'
}

function handleApprove(id) {
  ElMessageBox.confirm('确认通过该提现申请?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    await auditWithdrawal(id, { status: 2 })
    ElMessage.success('审核通过')
    fetchWithdrawalData()
  })
}

// ==================== 拒绝弹窗 ====================
const rejectDialog = reactive({
  visible: false,
  withdrawalId: null
})
const rejectForm = reactive({
  reason: ''
})
const rejectFormRef = ref()
const rejectRules = {
  reason: [{ required: true, message: '请输入拒绝原因', trigger: 'blur' }]
}

function handleReject(id) {
  rejectDialog.withdrawalId = id
  rejectForm.reason = ''
  rejectDialog.visible = true
}

function handleSubmitReject() {
  rejectFormRef.value.validate(async (valid) => {
    if (valid) {
      await auditWithdrawal(rejectDialog.withdrawalId, { status: 3, reason: rejectForm.reason })
      ElMessage.success('已拒绝')
      rejectDialog.visible = false
      fetchWithdrawalData()
    }
  })
}

function handleConfirmPay(id) {
  ElMessageBox.confirm('确认已完成打款?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    await auditWithdrawal(id, { status: 4 })
    ElMessage.success('已确认打款')
    fetchWithdrawalData()
  })
}

function handleTabChange(tab) {
  if (tab === 'wallet') {
    fetchWalletData()
  } else if (tab === 'withdrawal') {
    fetchWithdrawalData()
  }
}

onMounted(() => {
  fetchWalletData()
})
</script>
