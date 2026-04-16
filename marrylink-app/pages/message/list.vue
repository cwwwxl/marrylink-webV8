<template>
  <view class="message-container">
    <!-- 标签页 -->
    <view class="tabs">
      <view 
        class="tab-item" 
        :class="{ active: activeTab === 'all' }"
        @click="switchTab('all')"
      >
        <text class="tab-text">全部</text>
        <view v-if="activeTab === 'all'" class="tab-line"></view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: activeTab === 'unread' }"
        @click="switchTab('unread')"
      >
        <text class="tab-text">未读</text>
        <view v-if="unreadCount > 0" class="tab-badge">{{ unreadCount }}</view>
        <view v-if="activeTab === 'unread'" class="tab-line"></view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: activeTab === 'read' }"
        @click="switchTab('read')"
      >
        <text class="tab-text">已读</text>
        <view v-if="activeTab === 'read'" class="tab-line"></view>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view 
      class="message-list" 
      scroll-y 
      @scrolltolower="loadMore"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="messages.length === 0" class="empty-state">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无消息</text>
      </view>

      <view 
        v-else
        class="message-item" 
        v-for="message in messages" 
        :key="message.id"
        @click="handleMessageClick(message)"
      >
        <view class="message-dot" v-if="message.status === 1"></view>
        <view class="message-content">
          <text class="message-text">{{ message.content }}</text>
          <text class="message-time">{{ formatTime(message.createTime) }}</text>
        </view>
      </view>

      <view v-if="loading" class="loading-more">
        <text>加载中...</text>
      </view>

      <view v-if="!hasMore && messages.length > 0" class="no-more">
        <text>没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getMessageList, markMessagesAsRead, getUnreadCount } from '@/api/message'

export default {
  data() {
    return {
      activeTab: 'all', // all, unread, read
      messages: [],
      unreadCount: 0,
      current: 1,
      size: 20,
      loading: false,
      refreshing: false,
      hasMore: true
    }
  },

  onLoad() {
    this.loadMessages()
    this.loadUnreadCount()
  },

  methods: {
    // 切换标签
    switchTab(tab) {
      if (this.activeTab === tab) return
      this.activeTab = tab
      this.current = 1
      this.messages = []
      this.hasMore = true
      this.loadMessages()
    },

    // 加载消息列表
    async loadMessages() {
      if (this.loading || !this.hasMore) return

      try {
        this.loading = true
        const params = {
          current: this.current,
          size: this.size
        }

        // 根据标签设置状态筛选
        if (this.activeTab === 'unread') {
          params.status = 1
        } else if (this.activeTab === 'read') {
          params.status = 2
        }

        const res = await getMessageList(params)
        
        if (res.code === 200 || res.code === '00000') {
          const newMessages = res.data.records || []
          this.messages = this.current === 1 ? newMessages : [...this.messages, ...newMessages]
          this.hasMore = this.messages.length < res.data.total
          this.current++
        }
      } catch (error) {
        console.error('加载消息失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
        this.refreshing = false
      }
    },

    // 加载未读数量
    async loadUnreadCount() {
      try {
        const res = await getUnreadCount()
        if (res.code === 200 || res.code === '00000') {
          this.unreadCount = res.data.count || 0
        }
      } catch (error) {
        console.error('加载未读数量失败:', error)
      }
    },

    // 下拉刷新
    onRefresh() {
      this.refreshing = true
      this.current = 1
      this.messages = []
      this.hasMore = true
      this.loadMessages()
      this.loadUnreadCount()
    },

    // 加载更多
    loadMore() {
      if (!this.loading && this.hasMore) {
        this.loadMessages()
      }
    },

    // 点击消息
    async handleMessageClick(message) {
      if (message.status === 1) {
        try {
          await markMessagesAsRead([message.id])
          message.status = 2
          this.unreadCount = Math.max(0, this.unreadCount - 1)
          
          // 如果在未读标签页，从列表中移除该消息
          if (this.activeTab === 'unread') {
            const index = this.messages.findIndex(m => m.id === message.id)
            if (index !== -1) {
              this.messages.splice(index, 1)
            }
          }
        } catch (error) {
          console.error('标记已读失败:', error)
        }
      }
    },

    // 格式化时间
    formatTime(time) {
      const date = new Date(time)
      const now = new Date()
      const diff = now.getTime() - date.getTime()
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)
      
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      if (hours < 24) return `${hours}小时前`
      if (days < 7) return `${days}天前`
      
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  }
}
</script>

<style lang="scss" scoped>
.message-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.tabs {
  display: flex;
  background-color: #ffffff;
  padding: 0 32rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);

  .tab-item {
    flex: 1;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;

    .tab-text {
      font-size: 28rpx;
      color: #666666;
      transition: all 0.3s;
    }

    .tab-badge {
      position: absolute;
      top: 16rpx;
      right: 50%;
      transform: translateX(40rpx);
      min-width: 32rpx;
      height: 32rpx;
      line-height: 32rpx;
      padding: 0 8rpx;
      background-color: #ef4444;
      color: #ffffff;
      border-radius: 16rpx;
      font-size: 20rpx;
      text-align: center;
    }

    .tab-line {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 48rpx;
      height: 6rpx;
      background-color: #1d4ed8;
      border-radius: 3rpx;
    }

    &.active .tab-text {
      color: #1d4ed8;
      font-weight: bold;
    }
  }
}

.message-list {
  flex: 1;
  padding: 20rpx 0;
}

.message-item {
  background-color: #ffffff;
  margin: 0 32rpx 20rpx;
  padding: 32rpx;
  border-radius: 16rpx;
  position: relative;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .message-dot {
    position: absolute;
    top: 32rpx;
    left: 16rpx;
    width: 16rpx;
    height: 16rpx;
    background-color: #ef4444;
    border-radius: 50%;
  }

  .message-content {
    display: flex;
    flex-direction: column;

    .message-text {
      font-size: 28rpx;
      color: #333333;
      line-height: 1.6;
      margin-bottom: 16rpx;
    }

    .message-time {
      font-size: 24rpx;
      color: #999999;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 160rpx 0;

  .empty-icon {
    font-size: 120rpx;
    opacity: 0.3;
    margin-bottom: 24rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}

.loading-more,
.no-more {
  text-align: center;
  padding: 32rpx 0;
  font-size: 24rpx;
  color: #999999;
}
</style>