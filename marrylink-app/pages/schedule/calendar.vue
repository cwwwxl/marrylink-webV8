<template>
  <view class="calendar-container">
    <view class="header">
      <view class="header-content">
        <text class="title" v-if="isHost">查看档期</text>
        <text class="title" v-else>选择档期</text>
        <text class="subtitle">{{ hostInfo.name }}</text>
      </view>
    </view>

    <view class="month-selector">
      <view class="month-btn" @click="handlePrevMonth">
        <text class="icon">◀</text>
      </view>
      <text class="current-month">{{ currentYearMonth }}</text>
      <view class="month-btn" @click="handleNextMonth">
        <text class="icon">▶</text>
      </view>
    </view>

    <view class="legend">
      <view class="legend-item">
        <view class="dot pending"></view>
        <text>待确认</text>
      </view>
      <view class="legend-item">
        <view class="dot occupied"></view>
        <text>已占用</text>
      </view>
    </view>

    <view class="calendar">
      <view class="weekdays">
        <text class="weekday" v-for="day in weekdays" :key="day">{{ day }}</text>
      </view>
      <view class="days">
        <view
            v-for="(day, index) in calendarDays"
            :key="index"
            class="day-cell"
            :class="getDayClass(day)"
            @click="handleDayClick(day)"
        >
          <text class="day-number">{{ day.day }}</text>
          <text v-if="day.status" class="status-text" :class="day.status">
            {{ getStatusText(day.status) }}
          </text>
        </view>
      </view>
    </view>

    <!-- 预约 + 支付 弹框 -->
    <view v-if="showBookModal" class="modal-overlay" @click="closeModal">
      <view class="modal-content" @click.stop>

        <!-- 正常步骤：填写信息 -->
        <view v-if="!payLoading && !paySuccess">
          <view class="modal-header">
            <text class="modal-title">订单支付</text>
            <text class="modal-close" @click="closeModal">✕</text>
          </view>

          <view class="modal-body">
            <text class="modal-date">预约日期：{{ selectedDate }}</text>

            <view class="wedding-type-section">
              <text class="section-label">婚礼类型：</text>
              <radio-group @change="handleWeddingTypeChange">
                <label class="radio-item" v-for="type in weddingTypes" :key="type.value">
                  <radio :value="type.value" :checked="selectedWeddingType === type.value" />
                  <text>{{ type.label }}</text>
                </label>
              </radio-group>
            </view>

            <!-- 微信支付样式金额 -->
            <view class="price-section">
              <text class="section-label">订单总额</text>
              <text class="price-original">¥{{ totalPrice }}</text>
            </view>
            <view class="price-section deposit">
              <text class="section-label">定金支付（30%）</text>
              <text class="price">¥{{ depositPrice }}</text>
            </view>
            <view class="price-tip-text">
              <text>*仅需支付30%定金，剩余费用线下结算</text>
            </view>
          </view>

          <view class="modal-footer">
            <view class="modal-btn cancel" @click="closeModal">取消</view>
            <view class="modal-btn confirm" @click="confirmPayAndBook">确认支付</view>
          </view>
        </view>

        <!-- 支付中：加载状态 -->
        <view v-if="payLoading" class="pay-loading">
          <view class="loading-box">
            <view class="loading-spin"></view>
            <text>支付处理中，请稍候...</text>
          </view>
        </view>

        <!-- 支付成功：结果状态 -->
        <view v-if="paySuccess" class="pay-success">
          <view class="success-box">
            <view class="success-icon">✔</view>
            <text class="success-title">支付成功</text>
            <view class="success-btn" @click="closeAfterSuccess">完成</view>
          </view>
        </view>

      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getHostDetail, getHostSchedule, bookHost } from '@/api/host'

export default {
  data() {
    return {
      hostId: '',
      hostInfo: {},
      currentDate: new Date(),
      scheduleData: [],
      weekdays: ['日', '一', '二', '三', '四', '五', '六'],
      showBookModal: false,
      selectedDate: '',
      selectedWeddingType: '01',
      totalPrice: 688,
      weddingTypes: [
        { label: '中式婚礼', value: '01' },
        { label: '西式婚礼', value: '02' },
        { label: '主题婚礼', value: '03' },
        { label: '户外婚礼', value: '04' }
      ],
      payLoading: false,
      paySuccess: false
    }
  },

  computed: {
    ...mapState('user', ['userInfo']),
    isHost() {
      return this.userInfo && this.userInfo.userType === 'HOST'
    },
    // 计算定金金额（30%）
    depositPrice() {
      return (this.totalPrice * 0.3).toFixed(2)
    },
    currentYearMonth() {
      const year = this.currentDate.getFullYear()
      const month = this.currentDate.getMonth() + 1
      return `${year}年${month}月`
    },
    calendarDays() {
      const year = this.currentDate.getFullYear()
      const month = this.currentDate.getMonth()
      const firstDay = new Date(year, month, 1).getDay()
      const daysInMonth = new Date(year, month + 1, 0).getDate()
      const days = []

      for (let i = 0; i < firstDay; i++) {
        days.push({ day: '', date: '', disabled: true })
      }

      for (let i = 1; i <= daysInMonth; i++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
        const schedule = this.scheduleData.find(s => s.weddingDate === dateStr)
        const isPast = new Date(dateStr) < new Date(new Date().toDateString())

        days.push({
          day: i,
          date: dateStr,
          status: schedule ? this.getScheduleStatus(schedule.status) : null,
          disabled: isPast,
          schedule
        })
      }
      return days
    }
  },

  onLoad(options) {
    if (options.hostId) {
      this.hostId = options.hostId
      this.loadHostInfo()
      this.loadSchedule()
    }
  },

  methods: {
    async loadHostInfo() {
      try {
        const res = await getHostDetail(this.hostId)
        this.hostInfo = res.data || {}
        if (res.data && res.data.price) {
          this.totalPrice = res.data.price
        }
      } catch (error) {
        console.error('加载主持人信息失败:', error)
      }
    },
    async loadSchedule() {
      try {
        const year = this.currentDate.getFullYear()
        const month = this.currentDate.getMonth() + 1
        const res = await getHostSchedule({
          hostId: this.hostId,
          year,
          month
        })
        this.scheduleData = res.data || []
      } catch (error) {
        console.error('加载档期失败:', error)
      }
    },
    getScheduleStatus(status) {
      if (status === 3) return 'occupied'
      if (status === 1) return 'pending'
      return null
    },
    getStatusText(status) {
      if (status === 'occupied') return '已占用'
      if (status === 'pending') return '待确认'
    },
    getDayClass(day) {
      if (!day.day) return 'empty'
      if (day.disabled) return 'disabled'
      if (day.status === 'occupied') return 'occupied'
      if (day.status === 'pending') return 'pending'
      return 'available'
    },
    handlePrevMonth() {
      const date = new Date(this.currentDate)
      date.setMonth(date.getMonth() - 1)
      this.currentDate = date
      this.loadSchedule()
    },
    handleNextMonth() {
      const date = new Date(this.currentDate)
      date.setMonth(date.getMonth() + 1)
      this.currentDate = date
      this.loadSchedule()
    },
    handleDayClick(day) {
      if (!day.day || day.disabled) return
      if (this.isHost) return
      if (day.status === 'occupied' || day.status === 'pending') {
        uni.showToast({ title: '该日期已被预约', icon: 'none' })
        return
      }
      this.selectedDate = day.date
      this.selectedWeddingType = '01'
      this.showBookModal = true
      this.payLoading = false
      this.paySuccess = false
    },
    handleWeddingTypeChange(e) {
      this.selectedWeddingType = e.detail.value
    },
    closeModal() {
      this.showBookModal = false
      this.payLoading = false
      this.paySuccess = false
    },
    async confirmPayAndBook() {
      try {
        this.payLoading = true
        await bookHost({
          hostId: this.hostId,
          weddingDate: this.selectedDate,
          weddingType: this.selectedWeddingType,
          payAmount: parseFloat(this.depositPrice) // 只支付30%定金
        })
        this.payLoading = false
        this.paySuccess = true
        setTimeout(() => {
          this.closeAfterSuccess()
        }, 2000)
      } catch (error) {
        this.payLoading = false
        uni.showToast({
          title: error.message || '预约失败',
          icon: 'none'
        })
      }
    },
    closeAfterSuccess() {
      this.closeModal()
      this.loadSchedule()
    }
  }
}
</script>

<style lang="scss" scoped>
.calendar-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}
.header {
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  padding: 32rpx;
  color: #ffffff;
}
.header-content {
  .title {
    display: block;
    font-size: 40rpx;
    font-weight: bold;
    margin-bottom: 8rpx;
  }
  .subtitle {
    font-size: 28rpx;
    opacity: 0.9;
  }
}
.month-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  background-color: #ffffff;
  margin-bottom: 20rpx;
}
.month-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  border-radius: 50%;
  .icon {
    font-size: 24rpx;
    color: #1d4ed8;
  }
}
.current-month {
  font-size: 32rpx;
  font-weight: bold;
  color: #333333;
}
.legend {
  display: flex;
  justify-content: center;
  gap: 40rpx;
  padding: 24rpx 32rpx;
  background-color: #ffffff;
  margin-bottom: 20rpx;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  .dot {
    width: 24rpx;
    height: 24rpx;
    border-radius: 50%;
    &.pending {
      background-color: #e6a23c;
    }
    &.occupied {
      background-color: #1d4ed8;
    }
  }
  text {
    font-size: 24rpx;
    color: #666666;
  }
}
.calendar {
  background-color: #ffffff;
  padding: 32rpx;
}
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 20rpx;
}
.weekday {
  text-align: center;
  font-size: 26rpx;
  color: #999999;
  padding: 16rpx 0;
}
.days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 16rpx;
}
.day-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  .day-number {
    font-size: 28rpx;
    font-weight: 500;
    margin-bottom: 4rpx;
  }
  .status-text {
    font-size: 20rpx;
    &.pending {
      color: #e6a23c;
    }
    &.occupied {
      color: #1d4ed8;
    }
  }
  &.empty {
    visibility: hidden;
  }
  &.disabled .day-number {
    color: #cccccc;
  }
  &.available {
    background-color: #f0f9ff;
  }
  &.pending {
    background-color: #fef5e7;
  }
  &.occupied {
    background-color: #ecf5ff;
  }
}

/* 弹窗整体 */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-content {
  width: 90%;
  max-width: 600rpx;
  background-color: #ffffff;
  border-radius: 20rpx;
  overflow: hidden;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
  .modal-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
  }
  .modal-close {
    font-size: 40rpx;
    color: #999;
  }
}
.modal-body {
  padding: 32rpx;
  .modal-date {
    display: block;
    font-size: 28rpx;
    color: #333;
    margin-bottom: 24rpx;
  }
}
.wedding-type-section {
  .section-label {
    font-size: 28rpx;
    margin-bottom: 16rpx;
    color: #333;
  }
  .radio-item {
    display: flex;
    align-items: center;
    padding: 12rpx 0;
    radio {
      margin-right: 12rpx;
    }
  }
}

/* 微信支付风格金额 */
.price-section {
  margin-top: 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  .section-label {
    font-size: 28rpx;
    color: #333;
  }
  .price {
    font-size: 38rpx;
    font-weight: bold;
    color: #07c160;
  }
  .price-original {
    font-size: 28rpx;
    color: #999;
    text-decoration: line-through;
  }
  &.deposit {
    margin-top: 16rpx;
    padding: 16rpx 20rpx;
    background-color: #f0faf4;
    border-radius: 12rpx;
  }
}

.price-tip-text {
  margin-top: 12rpx;
  text {
    font-size: 22rpx;
    color: #999;
  }
}

.modal-footer {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
}
.modal-btn {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  &.cancel {
    color: #666;
    border-right: 1rpx solid #f0f0f0;
  }
  &.confirm {
    color: #fff;
    font-weight: bold;
    background-color: #07c160;
  }
}

/* 支付中 */
.pay-loading {
  padding: 80rpx 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}
.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.loading-spin {
  width: 50rpx;
  height: 50rpx;
  border: 4rpx solid #f0f0f0;
  border-top: 4rpx solid #07c160;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20rpx;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 支付成功 */
.pay-success {
  padding: 80rpx 40rpx;
  text-align: center;
}
.success-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.success-icon {
  width: 100rpx;
  height: 100rpx;
  background-color: #07c160;
  color: #fff;
  border-radius: 50%;
  font-size: 50rpx;
  line-height: 100rpx;
  text-align: center;
  margin-bottom: 30rpx;
}
.success-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 40rpx;
}
.success-btn {
  width: 220rpx;
  height: 80rpx;
  background-color: #07c160;
  color: #fff;
  border-radius: 12rpx;
  line-height: 80rpx;
  text-align: center;
  font-size: 28rpx;
}
</style>