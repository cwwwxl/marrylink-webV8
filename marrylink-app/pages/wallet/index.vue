<template>
  <view class="wallet-container">
    <view class="header">
      <text class="title">我的钱包</text>
    </view>

    <!-- 钱包概览 -->
    <view class="wallet-card" v-if="wallet">
      <view class="balance-section">
        <text class="balance-label">可用余额</text>
        <text class="balance-value">¥{{ availableBalance }}</text>
      </view>
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">¥{{ wallet.balance || '0.00' }}</text>
          <text class="stat-label">账户余额</text>
        </view>
        <view class="stat-item">
          <text class="stat-value frozen">¥{{ wallet.frozenAmount || '0.00' }}</text>
          <text class="stat-label">冻结(待付佣金)</text>
        </view>
      </view>
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">¥{{ wallet.totalIncome || '0.00' }}</text>
          <text class="stat-label">累计收入</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">¥{{ wallet.totalCommission || '0.00' }}</text>
          <text class="stat-label">累计佣金</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">¥{{ wallet.totalWithdrawn || '0.00' }}</text>
          <text class="stat-label">累计提现</text>
        </view>
      </view>
    </view>
    <view class="wallet-card empty-wallet" v-else>
      <text>暂无钱包信息，完成订单后将自动创建</text>
    </view>

    <!-- 操作按钮 -->
    <view class="action-section" v-if="wallet">
      <view class="action-btn" @click="showWithdrawModal = true">申请提现</view>
      <view class="action-btn secondary" @click="goToCommission">查看佣金</view>
    </view>

    <!-- 提现记录 -->
    <view class="section-title-bar">
      <text>提现记录</text>
    </view>
    <scroll-view class="list" scroll-y>
      <view v-if="withdrawals.length > 0">
        <view class="wd-card" v-for="item in withdrawals" :key="item.id">
          <view class="wd-header">
            <text class="wd-no">{{ item.withdrawalNo }}</text>
            <text class="wd-status" :class="'s-' + item.status">{{ getWdStatusText(item.status) }}</text>
          </view>
          <view class="wd-body">
            <text class="wd-amount">¥{{ item.amount }}</text>
            <text class="wd-info">{{ item.accountType }} {{ item.accountNo }}</text>
            <text class="wd-time">{{ formatDate(item.createTime) }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty"><text>暂无提现记录</text></view>
    </scroll-view>

    <!-- 提现弹窗 -->
    <view v-if="showWithdrawModal" class="modal-overlay" @click="showWithdrawModal = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">申请提现</text>
          <text class="modal-close" @click="showWithdrawModal = false">x</text>
        </view>
        <view class="modal-body">
          <text class="form-label">可提现金额: ¥{{ availableBalance }}</text>
          <view class="form-group">
            <text class="form-label">提现金额</text>
            <input class="form-input" type="digit" v-model="withdrawForm.amount" placeholder="请输入提现金额" />
          </view>
          <view class="form-group">
            <text class="form-label">账户类型</text>
            <radio-group @change="e => withdrawForm.accountType = e.detail.value">
              <label class="radio-item"><radio value="支付宝" :checked="withdrawForm.accountType === '支付宝'" /><text>支付宝</text></label>
              <label class="radio-item"><radio value="微信" :checked="withdrawForm.accountType === '微信'" /><text>微信</text></label>
              <label class="radio-item"><radio value="银行卡" :checked="withdrawForm.accountType === '银行卡'" /><text>银行卡</text></label>
            </radio-group>
          </view>
          <view class="form-group">
            <text class="form-label">账号</text>
            <input class="form-input" v-model="withdrawForm.accountNo" placeholder="请输入账号" />
          </view>
          <view class="form-group">
            <text class="form-label">户名</text>
            <input class="form-input" v-model="withdrawForm.accountName" placeholder="请输入户名" />
          </view>
        </view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="showWithdrawModal = false">取消</view>
          <view class="modal-btn confirm" @click="handleWithdraw">确认提现</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getMyWallet, submitWithdrawal, getWithdrawalList } from '@/api/order'

export default {
  data() {
    return {
      wallet: null,
      withdrawals: [],
      showWithdrawModal: false,
      withdrawForm: {
        amount: '',
        accountType: '支付宝',
        accountNo: '',
        accountName: ''
      }
    }
  },
  computed: {
    availableBalance() {
      if (!this.wallet) return '0.00'
      const bal = parseFloat(this.wallet.balance || 0)
      const frozen = parseFloat(this.wallet.frozenAmount || 0)
      return (bal - frozen).toFixed(2)
    }
  },
  onLoad() {
    this.loadWallet()
    this.loadWithdrawals()
  },
  methods: {
    async loadWallet() {
      try {
        const res = await getMyWallet()
        if (res.code === 200 || res.code === '00000') {
          this.wallet = res.data
        }
      } catch (e) { console.error(e) }
    },
    async loadWithdrawals() {
      try {
        const res = await getWithdrawalList({ current: 1, size: 50 })
        if (res.code === 200 || res.code === '00000') {
          this.withdrawals = res.data.records || []
        }
      } catch (e) { console.error(e) }
    },
    async handleWithdraw() {
      const amount = parseFloat(this.withdrawForm.amount)
      if (!amount || amount <= 0) {
        uni.showToast({ title: '请输入正确金额', icon: 'none' })
        return
      }
      if (amount > parseFloat(this.availableBalance)) {
        uni.showToast({ title: '超出可提现金额', icon: 'none' })
        return
      }
      if (!this.withdrawForm.accountNo || !this.withdrawForm.accountName) {
        uni.showToast({ title: '请填写完整信息', icon: 'none' })
        return
      }
      try {
        const res = await submitWithdrawal(this.withdrawForm)
        if (res.code === 200 || res.code === '00000') {
          uni.showToast({ title: '提现申请已提交', icon: 'success' })
          this.showWithdrawModal = false
          this.withdrawForm = { amount: '', accountType: '支付宝', accountNo: '', accountName: '' }
          this.loadWallet()
          this.loadWithdrawals()
        }
      } catch (e) { /* handled */ }
    },
    goToCommission() {
      uni.navigateTo({ url: '/pages/commission/index' })
    },
    getWdStatusText(s) {
      return { 1: '待审核', 2: '已通过', 3: '已拒绝', 4: '已打款' }[s] || '未知'
    },
    formatDate(dt) {
      if (!dt) return ''
      return dt.replace('T', ' ').substring(0, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
.wallet-container { min-height: 100vh; background: #f5f7fa; }
.header { background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%); padding: 32rpx; .title { font-size: 36rpx; font-weight: bold; color: #fff; } }
.wallet-card {
  margin: 20rpx; background: #fff; border-radius: 16rpx; padding: 32rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05);
  &.empty-wallet { text-align: center; color: #999; padding: 60rpx; }
  .balance-section { text-align: center; margin-bottom: 24rpx;
    .balance-label { display: block; font-size: 26rpx; color: #666; margin-bottom: 8rpx; }
    .balance-value { display: block; font-size: 56rpx; font-weight: bold; color: #1d4ed8; }
  }
  .stats-row { display: flex; justify-content: space-around; padding: 16rpx 0; border-top: 1rpx solid #f0f2f5;
    .stat-item { text-align: center;
      .stat-value { display: block; font-size: 28rpx; font-weight: bold; color: #333; &.frozen { color: #f59e0b; } }
      .stat-label { display: block; font-size: 22rpx; color: #999; margin-top: 4rpx; }
    }
  }
}
.action-section {
  display: flex; gap: 20rpx; padding: 0 20rpx; margin-bottom: 20rpx;
  .action-btn { flex: 1; text-align: center; padding: 20rpx; background: #07c160; color: #fff; border-radius: 12rpx; font-size: 28rpx; font-weight: bold;
    &.secondary { background: #1d4ed8; }
  }
}
.section-title-bar { padding: 20rpx 32rpx; font-size: 30rpx; font-weight: bold; color: #333; }
.list { padding: 0 20rpx; }
.wd-card {
  background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx;
  .wd-header { display: flex; justify-content: space-between; margin-bottom: 12rpx;
    .wd-no { font-size: 24rpx; color: #999; }
    .wd-status { font-size: 24rpx; font-weight: bold;
      &.s-1 { color: #f59e0b; } &.s-2 { color: #3b82f6; } &.s-3 { color: #ef4444; } &.s-4 { color: #10b981; }
    }
  }
  .wd-body {
    .wd-amount { display: block; font-size: 32rpx; font-weight: bold; color: #333; margin-bottom: 8rpx; }
    .wd-info { display: block; font-size: 24rpx; color: #666; margin-bottom: 4rpx; }
    .wd-time { display: block; font-size: 22rpx; color: #999; }
  }
}
.empty { text-align: center; padding: 100rpx 0; color: #999; }
/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { width: 90%; max-width: 640rpx; background: #fff; border-radius: 20rpx; overflow: hidden; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 32rpx; border-bottom: 1rpx solid #f0f0f0;
  .modal-title { font-size: 32rpx; font-weight: bold; }
  .modal-close { font-size: 36rpx; color: #999; }
}
.modal-body { padding: 24rpx 32rpx;
  .form-label { display: block; font-size: 26rpx; color: #666; margin-bottom: 12rpx; margin-top: 16rpx; }
  .form-input { width: 100%; height: 72rpx; padding: 0 20rpx; background: #f5f7fa; border-radius: 8rpx; font-size: 28rpx; box-sizing: border-box; }
  .form-group { margin-bottom: 8rpx; }
  .radio-item { display: inline-flex; align-items: center; margin-right: 24rpx; font-size: 26rpx; }
}
.modal-footer { display: flex; border-top: 1rpx solid #f0f0f0;
  .modal-btn { flex: 1; height: 88rpx; display: flex; align-items: center; justify-content: center; font-size: 28rpx;
    &.cancel { color: #666; border-right: 1rpx solid #f0f0f0; }
    &.confirm { color: #fff; font-weight: bold; background: #07c160; }
  }
}
</style>
