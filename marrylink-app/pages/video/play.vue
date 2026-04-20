<template>
  <view class="play-container">
    <!-- 返回按钮 -->
    <view class="back-btn" @click="goBack">
      <text class="back-icon">‹</text>
      <text class="back-text">返回</text>
    </view>

    <!-- 视频播放器 -->
    <view class="player-section" v-if="videoInfo">
      <video
        :src="BASE_URL + videoInfo.videoUrl"
        class="video-player"
        controls
        autoplay
        :poster="videoInfo.coverUrl ? BASE_URL + videoInfo.coverUrl : ''"
        object-fit="contain"
      ></video>
    </view>

    <!-- 视频信息 -->
    <view class="info-section" v-if="videoInfo">
      <text class="video-title">{{ videoInfo.title }}</text>
      <text class="video-desc" v-if="videoInfo.description">{{ videoInfo.description }}</text>
      <view class="host-info" v-if="videoInfo.hostName">
        <text class="host-label">主持人：</text>
        <text class="host-name">{{ videoInfo.hostName }}</text>
      </view>
    </view>

    <!-- 加载状态 -->
    <view class="loading-state" v-if="loading">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 错误状态 -->
    <view class="empty-state" v-if="!loading && !videoInfo">
      <text class="empty-icon">🎬</text>
      <text class="empty-text">视频不存在或已被删除</text>
    </view>
  </view>
</template>

<script>
import { getVideoDetail, BASE_URL } from '@/api/video'

export default {
  data() {
    return {
      BASE_URL: BASE_URL,
      videoId: '',
      videoInfo: null,
      loading: true
    }
  },

  onLoad(options) {
    this.videoId = options.id
    if (this.videoId) {
      this.loadVideoDetail()
    }
  },

  methods: {
    async loadVideoDetail() {
      this.loading = true
      try {
        const res = await getVideoDetail(this.videoId)
        if (res.code === 200 || res.code === '00000') {
          this.videoInfo = res.data
        }
      } catch (error) {
        console.error('加载视频详情失败:', error)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.play-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 100rpx;
}

.back-btn {
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  background: #ffffff;

  .back-icon {
    font-size: 48rpx;
    color: #1d4ed8;
    margin-right: 8rpx;
    line-height: 1;
  }

  .back-text {
    font-size: 28rpx;
    color: #1d4ed8;
  }
}

.player-section {
  width: 100%;
  background: #000000;

  .video-player {
    width: 100%;
    height: 450rpx;
  }
}

.info-section {
  background: #ffffff;
  padding: 32rpx;
  margin: 24rpx 32rpx;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);

  .video-title {
    display: block;
    font-size: 36rpx;
    font-weight: bold;
    color: #333333;
    margin-bottom: 16rpx;
  }

  .video-desc {
    display: block;
    font-size: 28rpx;
    color: #666666;
    line-height: 1.6;
    margin-bottom: 24rpx;
  }

  .host-info {
    display: flex;
    align-items: center;
    padding-top: 24rpx;
    border-top: 1rpx solid #e5e7eb;

    .host-label {
      font-size: 26rpx;
      color: #999999;
    }

    .host-name {
      font-size: 26rpx;
      color: #1d4ed8;
      font-weight: bold;
    }
  }
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;

  .loading-text {
    font-size: 28rpx;
    color: #999999;
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
    opacity: 0.3;
    margin-bottom: 24rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}
</style>
