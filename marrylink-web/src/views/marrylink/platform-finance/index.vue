<template>
  <div class="app-container">
    <!-- 平台账户概览 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">可提现余额</div>
            <div class="stat-value primary">¥{{ formatMoney(account.balance) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">累计佣金收入</div>
            <div class="stat-value success">¥{{ formatMoney(account.totalCommissionIncome) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">累计已提现</div>
            <div class="stat-value warning">¥{{ formatMoney(account.totalWithdrawn) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作按钮 -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <el-button type="primary" icon="money" @click="showWithdrawDialog">平台提现</el-button>
      <el-button icon="refresh" @click="fetchAccount">刷新账户</el-button>
    </el-card>

    <!-- 提现记录 -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>提现记录</span>
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px;" @change="handleQuery">
            <el-option label="待处理" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </div>
      </template>

      <el-table v-loading="loading" :data="pageData" border stripe>
        <el-table-column label="提现单号" prop="withdrawalNo" width="200" />
        <el-table-column label="提现金额" width="140">
          <template #default="scope">
            <span style="color: #e6a23c; font-weight: bold;">¥{{ formatMoney(scope.row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="账户类型" prop="accountType" width="100" />
        <el-table-column label="账号" prop="accountNo" width="180" />
        <el-table-column label="户名" prop="accountName" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'warning' : 'success'">
              {{ scope.row.status === 1 ? '待处理' : '已完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作人" prop="operator" width="100" />
        <el-table-column label="申请时间" prop="createTime" width="170" />
        <el-table-column label="完成时间" prop="completeTime" width="170" />
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="scope">
            <template v-if="scope.row.status === 1">
              <el-button type="success" link size="small" @click="handleComplete(scope.row.id)">确认完成</el-button>
              <el-button type="danger" link size="small" @click="handleCancel(scope.row.id)">取消</el-button>
            </template>
            <span v-else style="color: #999;">已处理</span>
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

    <!-- 提现弹窗 -->
    <el-dialog v-model="withdrawDialog.visible" title="平台提现" width="500px">
      <el-form :model="withdrawForm" label-width="80px">
        <el-form-item label="可提现">
          <span style="font-size: 18px; font-weight: bold; color: #409eff;">¥{{ formatMoney(account.balance) }}</span>
        </el-form-item>
        <el-form-item label="提现金额" required>
          <el-input-number v-model="withdrawForm.amount" :min="0.01" :max="parseFloat(account.balance) || 0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="账户类型" required>
          <el-radio-group v-model="withdrawForm.accountType">
            <el-radio label="银行卡">银行卡</el-radio>
            <el-radio label="支付宝">支付宝</el-radio>
            <el-radio label="微信">微信</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="账号" required>
          <el-input v-model="withdrawForm.accountNo" placeholder="请输入收款账号" />
        </el-form-item>
        <el-form-item label="户名" required>
          <el-input v-model="withdrawForm.accountName" placeholder="请输入户名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="withdrawForm.remark" type="textarea" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleWithdraw" :loading="withdrawDialog.loading">确认提现</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Pagination from '@/components/Pagination/index.vue'
import {
  getPlatformAccount,
  submitPlatformWithdrawal,
  getPlatformWithdrawalPage,
  completePlatformWithdrawal,
  cancelPlatformWithdrawal
} from '@/api/marrylink-api'

const account = reactive({
  balance: '0.00',
  totalCommissionIncome: '0.00',
  totalWithdrawn: '0.00'
})

const queryParams = reactive({ current: 1, size: 10, status: null })
const pageData = ref([])
const total = ref(0)
const loading = ref(false)

const withdrawDialog = reactive({ visible: false, loading: false })
const withdrawForm = reactive({
  amount: 0,
  accountType: '银行卡',
  accountNo: '',
  accountName: '',
  remark: ''
})

function formatMoney(val) {
  return Number(val || 0).toFixed(2)
}

async function fetchAccount() {
  try {
    const res = await getPlatformAccount()
    Object.assign(account, res)
  } catch (e) {
    console.error(e)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPlatformWithdrawalPage(queryParams)
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

function showWithdrawDialog() {
  withdrawForm.amount = 0
  withdrawForm.accountType = '银行卡'
  withdrawForm.accountNo = ''
  withdrawForm.accountName = ''
  withdrawForm.remark = ''
  withdrawDialog.visible = true
}

async function handleWithdraw() {
  if (!withdrawForm.amount || withdrawForm.amount <= 0) {
    return ElMessage.warning('请输入提现金额')
  }
  if (!withdrawForm.accountNo || !withdrawForm.accountName) {
    return ElMessage.warning('请填写完整账户信息')
  }
  withdrawDialog.loading = true
  try {
    await submitPlatformWithdrawal(withdrawForm)
    ElMessage.success('提现申请已提交')
    withdrawDialog.visible = false
    fetchAccount()
    fetchData()
  } finally {
    withdrawDialog.loading = false
  }
}

function handleComplete(id) {
  ElMessageBox.confirm('确认该笔提现已完成打款？', '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    await completePlatformWithdrawal(id)
    ElMessage.success('已标记完成')
    fetchAccount()
    fetchData()
  })
}

function handleCancel(id) {
  ElMessageBox.confirm('取消提现将退回金额到平台账户，确认取消？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await cancelPlatformWithdrawal(id)
    ElMessage.success('已取消，金额已退回')
    fetchAccount()
    fetchData()
  })
}

onMounted(() => {
  fetchAccount()
  fetchData()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 10px 0;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
}
.stat-value.primary { color: #409eff; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
