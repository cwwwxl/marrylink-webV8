<template>
  <view class="questionnaire-container">
    <!-- 顶部提示 -->
    <view class="tip-banner" v-if="!isHost">
      <text class="tip-icon">💡</text>
      <text class="tip-text">填写问卷可以帮助主持人更好地了解您的需求</text>
    </view>
    
    <!-- 问卷列表 -->
    <scroll-view class="questionnaire-list" scroll-y>
      <view 
        class="questionnaire-item" 
        v-for="item in questionnaireList" 
        :key="item.id"
        @click="goToFill(item)"
      >
        <view class="item-header">
          <view class="item-title-row">
            <text class="item-icon">📝</text>
            <text class="item-title">{{ item.submissionName || item.questionnaireName || '问卷' }}</text>
          </view>
          <view class="item-status" :class="getStatusClass(item.status)">
            {{ getStatusText(item.status) }}
          </view>
        </view>
        
        <text class="item-desc">{{ item.description }}</text>
        
        <view class="item-footer">
          <view class="item-info">
            <text class="info-item">📊 {{ item.questionCount || 10 }}道题</text>
            <text class="info-item">⏱️ 约{{ item.estimatedTime || 5 }}分钟</text>
          </view>
          <text class="item-arrow">›</text>
        </view>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-if="questionnaireList.length === 0 && !loading">
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无问卷</text>
      </view>
    </scroll-view>
    
    <!-- 我的提交记录 -->
    <view class="my-submissions" v-if="!isHost">
      <view class="submissions-header">
        <text class="submissions-title">我的提交记录</text>
        <!-- 查看全部功能已屏蔽 -->
      </view>
      
      <scroll-view class="submissions-list" scroll-y v-if="mySubmissions.length > 0">
        <view
          class="submission-item"
          v-for="item in mySubmissions"
          :key="item.id"
          @click="viewSubmission(item.id)"
        >
          <view class="submission-info">
            <text class="submission-title">{{ item.submissionName || item.questionnaireName || '问卷' }}</text>
            <text class="submission-date">提交于 {{ formatSubmitTime(item.createTime || item.submitTime) }}</text>
          </view>
          <view class="submission-status" :class="getStatusClass(item.status)">
            {{ getStatusText(item.status) }}
          </view>
        </view>
      </scroll-view>
      
      <view class="empty-submissions" v-else>
        <text>暂无提交记录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getQuestionnaireList, getMySubmissions, updateQuestionnaireStatus } from '@/api/questionnaire'

export default {
  data() {
    return {
      questionnaireList: [],
      mySubmissions: [],
      loading: false
    }
  },
  
  onShow() {
    console.log(this.token)
    if (!this.isLoggedIn) {
      this.loadData()
    }
  },



  computed: {
    ...mapState('user', ['userInfo', 'token']),

    isLoggedIn() {
      return !this.token
    },

        // 判断是否为主持人
    isHost() {
      return this.userInfo && this.userInfo.userType === 'HOST'
    },
    
    // 主持人姓名
    hostName() {
      return this.userInfo?.realName || '主持人'
    },
    
  },
  
  methods: {
    // 加载数据
    async loadData() {
      this.loading = true
      try {
        await Promise.all([
          this.loadQuestionnaireList(),
          this.loadMySubmissions()
        ])
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 加载问卷列表
    async loadQuestionnaireList() {
      try {
        const res = await getQuestionnaireList({ current: 1, size: 20})
        this.questionnaireList = res.data.records || []
      } catch (error) {
        console.error('加载问卷列表失败:', error)
      }
    },
    
    // 加载我的提交记录
    async loadMySubmissions() {
      try {
        const res = await getMySubmissions({ current: 1, size: 5 })
        this.mySubmissions = res.data.records || []
      } catch (error) {
        console.error('加载提交记录失败:', error)
      }
    },
    
    // 获取状态样式类
    getStatusClass(status) {
      if (this.isHost) {
        const classMap = {
          '2': 'status-pending',
          '3': 'status-submitted',
        }
        return classMap[status] || 'status-pending'
      }
      const classMap = {
        '1': 'status-pending',
        '2': 'status-submitted',
        '3': 'status-submitted',
      }
      return classMap[status] || 'status-pending'
    },
    
    // 获取状态文本
    getStatusText(status) {
      if (this.isHost) {
        const textMap = {
          '2': '待查看',
          '3': '已查看',
        }
        return textMap[status] || '待查看'
      }
      const textMap = {
        '1': '待填写',
        '2': '已提交',
        '3': '已提交',
      }
      return textMap[status] || '待填写'
    },
    
    // 格式化提交时间
    formatSubmitTime(dateStr) {
      if (!dateStr) return ''
      
      try {
        const date = new Date(dateStr)
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        
        return `${year}-${month}-${day} ${hours}:${minutes}`
      } catch (error) {
        return dateStr
      }
    },
    
    // 跳转到填写问卷
    async goToFill(item) {
      // 检查登录状态
      const token = uni.getStorageSync('token')
      if (!token) {
        uni.showModal({
          title: '提示',
          content: '请先登录',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({
                url: '/pages/login/index'
              })
            }
          }
        })
        return
      }
      
      // 主持人模式下更新状态为3
      if (this.isHost) {
        try {
          await updateQuestionnaireStatus({ id: item.id, status: 3 })
          // 更新本地列表中的状态
          const index = this.questionnaireList.findIndex(q => q.id === item.id)
          if (index !== -1) {
            this.questionnaireList[index].status = 3
          }
        } catch (error) {
          console.error('更新问卷状态失败:', error)
        }
      }
      
      // 将整个item对象存储到缓存中
      uni.setStorageSync('currentQuestionnaire', item)
      
      uni.navigateTo({
        url: `/pages/questionnaire/fill?id=${item.templateId}`
      })
    },
    
    // 跳转到我的提交记录
    goToMySubmissions() {
      uni.showToast({
        title: '功能开发中',
        icon: 'none'
      })
    },
    
    // 查看提交详情
    viewSubmission(id) {
      uni.showToast({
        title: '功能开发中',
        icon: 'none'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.questionnaire-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.tip-banner {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  
  .tip-icon {
    font-size: 32rpx;
    margin-right: 12rpx;
  }
  
  .tip-text {
    flex: 1;
    font-size: 26rpx;
    color: #ffffff;
  }
}

.questionnaire-list {
  flex: 1;
  padding: 20rpx 32rpx;
  
  .questionnaire-item {
    background-color: #ffffff;
    border-radius: 16rpx;
    padding: 32rpx;
    margin-bottom: 20rpx;
    
    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16rpx;
      
      .item-title-row {
        display: flex;
        align-items: center;
        flex: 1;
        
        .item-icon {
          font-size: 32rpx;
          margin-right: 12rpx;
        }
        
        .item-title {
          font-size: 32rpx;
          font-weight: bold;
          color: #333333;
        }
      }
      
      .item-status {
        padding: 8rpx 20rpx;
        border-radius: 20rpx;
        font-size: 22rpx;
        
        &.status-pending {
          background-color: rgba(239, 68, 68, 0.1);
          color: #ef4444;
        }
        
        &.status-submitted {
          background-color: rgba(16, 185, 129, 0.1);
          color: #10b981;
        }
        
        &.status-reviewed {
          background-color: rgba(107, 114, 128, 0.1);
          color: #6b7280;
        }
      }
    }
    
    .item-desc {
      font-size: 26rpx;
      color: #666666;
      line-height: 1.6;
      margin-bottom: 20rpx;
    }
    
    .item-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .item-info {
        display: flex;
        
        .info-item {
          font-size: 24rpx;
          color: #999999;
          margin-right: 24rpx;
        }
      }
      
      .item-arrow {
        font-size: 48rpx;
        color: #1d4ed8;
        font-weight: 300;
      }
    }
  }
  
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 120rpx 0;
    
    .empty-icon {
      font-size: 120rpx;
      margin-bottom: 24rpx;
      opacity: 0.3;
    }
    
    .empty-text {
      font-size: 28rpx;
      color: #999999;
    }
  }
}

.my-submissions {
  background-color: #ffffff;
  padding: 32rpx;
  
  .submissions-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    
    .submissions-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333333;
      position: relative;
      padding-left: 20rpx;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 8rpx;
        height: 32rpx;
        background-color: #1d4ed8;
        border-radius: 4rpx;
      }
    }
    
    .submissions-more {
      font-size: 24rpx;
      color: #1d4ed8;
    }
  }
  
  .submissions-list {
    max-height: 400rpx;
    
    .submission-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24rpx;
      background-color: #f5f7fa;
      border-radius: 12rpx;
      margin-bottom: 16rpx;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .submission-info {
        flex: 1;
        
        .submission-title {
          display: block;
          font-size: 28rpx;
          color: #333333;
          margin-bottom: 8rpx;
        }
        
        .submission-date {
          font-size: 22rpx;
          color: #999999;
        }
      }
      
      .submission-status {
        padding: 8rpx 20rpx;
        border-radius: 20rpx;
        font-size: 22rpx;
        
        &.status-pending {
          background-color: rgba(239, 68, 68, 0.1);
          color: #ef4444;
        }
        
        &.status-submitted {
          background-color: rgba(16, 185, 129, 0.1);
          color: #10b981;
        }
        
        &.status-reviewed {
          background-color: rgba(107, 114, 128, 0.1);
          color: #6b7280;
        }
      }
    }
  }
  
  .empty-submissions {
    text-align: center;
    padding: 60rpx 0;
    font-size: 26rpx;
    color: #999999;
  }
}
</style>