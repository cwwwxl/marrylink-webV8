<template>
  <view class="commission-container">
    <view class="header">
      <text class="title">佣金管理</text>
    </view>

    <view class="tabs">
      <view class="tab-item" :class="{ active: currentStatus === null }" @click="changeTab(null)">全部</view>
      <view class="tab-item" :class="{ active: currentStatus === 1 }" @click="changeTab(1)">待支付</view>
      <view class="tab-item" :class="{ active: currentStatus === 2 }" @click="changeTab(2)">已支付</view>
      <view class="tab-item" :class="{ active: currentStatus === 3 }" @click="changeTab(3)">逾期</view>
    </view>

    <scroll-view class="list" scroll-y @scrolltolower="loadMore">
      <view v-if="list.length > 0">
        <view class="card" v-for="item in list" :key="item.id">
          <view class="card-header">
            <text class="commission-no">{{ item.commissionNo }}</text>
            <text class="status" :class="'status-' + item.status">{{ getStatusText(item.status) }}</text>
          </view>
          <view class="card-body">
            <view class="info-row">
              <text class="label">订单号:</text>
              <text class="value">{{ item.orderNo }}</text>
            </view>
            <view class="info-row">
              <text class="label">订单金额:</text>
              <text class="value">¥{{ item.orderAmount }}</text>
            </view>
            <view class="info-row">
              <text class="label">佣金比例:</text>
              <text class="value">{{ item.commissionRate }}%</text>
            </view>
            <view class="info-row highlight">
              <text class="label">佣金金额:</text>
              <text class="value amount">¥{{ item.commissionAmount }}</text>
            </view>
            <view class="info-row" v-if="item.deadline">
              <text class="label">截止日期:</text>
              <text class="value" :class="{ overdue: isOverdue(item) }">{{ formatDate(item.deadline) }}</text>
            </view>
          </view>
          <view class="card-footer" v-if="item.status === 1 || item.status === 3">
            <view class="pay-btn" @click="handlePay(item)">支付佣金</view>
          </view>
        </view>
      </view>
      <view v-else class="empty">
        <text>暂无佣金记录</text>
      </view>
      <view v-if="loading" class="loading">加载中...</view>
      <view v-if="noMore && list.length > 0" class="no-more">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script>
import { getCommissionList, payCommission } from '@/api/order'

export default {
  data() {
    return {
      currentStatus: null,
      list: [],
      current: 1,
      size: 10,
      loading: false,
      noMore: false
    }
  },
  onLoad() {
    this.loadList()
  },
  methods: {
    async loadList() {
      if (this.loading || this.noMore) return
      this.loading = true
      try {
        const params = { current: this.current, size: this.size }
        if (this.currentStatus !== null) params.status = this.currentStatus
        const res = await getCommissionList(params)
        if (res.code === 200 || res.code === '00000') {
          const newList = res.data.records || []
          this.list = this.current === 1 ? newList : [...this.list, ...newList]
          this.noMore = this.list.length >= res.data.total
        }
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    changeTab(status) {
      this.currentStatus = status
      this.current = 1
      this.list = []
      this.noMore = false
      this.loadList()
    },
    loadMore() {
      if (!this.noMore && !this.loading) {
        this.current++
        this.loadList()
      }
    },
    getStatusText(status) {
      return { 1: '待支付', 2: '已支付', 3: '逾期' }[status] || '未知'
    },
    formatDate(dt) {
      if (!dt) return ''
      return dt.replace('T', ' ').substring(0, 16)
    },
    isOverdue(item) {
      return item.status === 3 || (item.status === 1 && new Date(item.deadline) < new Date())
    },
    handlePay(item) {
      uni.showModal({
        title: '确认支付',
        content: `确定支付佣金 ¥${item.commissionAmount} 吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              const result = await payCommission(item.id)
              if (result.code === 200 || result.code === '00000') {
                uni.showToast({ title: '支付成功', icon: 'success' })
                this.current = 1
                this.list = []
                this.noMore = false
                this.loadList()
              }
            } catch (e) { /* handled by request.js */ }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.commission-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}
.header {
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  padding: 32rpx;
  .title { font-size: 36rpx; font-weight: bold; color: #fff; }
}
.tabs {
  display: flex;
  background: #fff;
  padding: 16rpx 0;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
  .tab-item {
    flex: 1; text-align: center; font-size: 28rpx; color: #666; padding: 16rpx 0; position: relative;
    &.active {
      color: #1d4ed8; font-weight: bold;
      &::after { content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 60rpx; height: 6rpx; background: #1d4ed8; border-radius: 3rpx; }
    }
  }
}
.list { flex: 1; padding: 20rpx; }
.card {
  background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.05);
  .card-header {
    display: flex; justify-content: space-between; align-items: center; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f2f5; margin-bottom: 16rpx;
    .commission-no { font-size: 24rpx; color: #999; }
    .status { font-size: 26rpx; font-weight: bold; padding: 6rpx 16rpx; border-radius: 16rpx;
      &.status-1 { color: #f59e0b; background: #fef3c7; }
      &.status-2 { color: #10b981; background: #d1fae5; }
      &.status-3 { color: #ef4444; background: #fee2e2; }
    }
  }
  .card-body {
    .info-row {
      display: flex; justify-content: space-between; padding: 8rpx 0;
      .label { font-size: 26rpx; color: #666; }
      .value { font-size: 26rpx; color: #333;
        &.amount { color: #ef4444; font-weight: bold; font-size: 30rpx; }
        &.overdue { color: #ef4444; }
      }
      &.highlight { padding: 12rpx 16rpx; background: #fef2f2; border-radius: 8rpx; margin-top: 8rpx; }
    }
  }
  .card-footer {
    margin-top: 16rpx; padding-top: 16rpx; border-top: 1rpx solid #f0f2f5; display: flex; justify-content: flex-end;
    .pay-btn {
      padding: 12rpx 40rpx; background: #07c160; color: #fff; border-radius: 12rpx; font-size: 28rpx; font-weight: bold;
    }
  }
}
.empty { text-align: center; padding: 200rpx 0; color: #999; font-size: 28rpx; }
.loading, .no-more { text-align: center; padding: 30rpx; font-size: 24rpx; color: #999; }
</style>
