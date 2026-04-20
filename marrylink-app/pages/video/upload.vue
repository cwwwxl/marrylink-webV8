<template>
  <view class="upload-container">
    <!-- 上传区域 -->
    <view class="upload-section">
      <text class="section-title">上传案例视频</text>

      <view class="upload-area" @click="chooseVideo" v-if="!videoPath">
        <text class="upload-icon">📹</text>
        <text class="upload-text">点击选择视频</text>
        <text class="upload-hint">支持mp4格式，最长5分钟</text>
      </view>

      <view class="video-preview" v-else>
        <video :src="videoPath" class="preview-video" :controls="true" object-fit="contain"></video>
        <view class="remove-btn" @click="removeVideo">
          <text class="remove-text">移除</text>
        </view>
      </view>

      <view class="form-group">
        <text class="form-label">视频标题 <text class="required">*</text></text>
        <input
          class="form-input"
          v-model="title"
          placeholder="请输入视频标题"
          maxlength="50"
        />
      </view>

      <view class="form-group">
        <text class="form-label">视频描述</text>
        <textarea
          class="form-textarea"
          v-model="description"
          placeholder="请输入视频描述（选填）"
          maxlength="200"
        ></textarea>
      </view>

      <!-- 上传进度 -->
      <view class="progress-bar" v-if="uploading">
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: uploadProgress + '%' }"></view>
        </view>
        <text class="progress-text">上传中 {{ uploadProgress }}%</text>
      </view>

      <button
        class="upload-btn"
        :class="{ disabled: uploading }"
        @click="handleUpload"
        :disabled="uploading"
      >
        {{ uploading ? '上传中...' : '上传视频' }}
      </button>
    </view>

    <!-- 我的视频列表 -->
    <view class="my-videos-section">
      <text class="section-title">我的案例视频</text>

      <view v-if="myVideos.length > 0" class="video-list">
        <view
          class="video-card"
          v-for="video in myVideos"
          :key="video.id"
        >
          <view class="video-card-header">
            <text class="video-card-title">{{ video.title }}</text>
            <view class="status-badge" :class="getStatusClass(video.status)">
              <text>{{ getStatusText(video.status) }}</text>
            </view>
          </view>
          <text class="video-card-desc" v-if="video.description">{{ video.description }}</text>
          <view class="video-card-footer">
            <text class="video-card-time">{{ formatTime(video.createTime) }}</text>
            <view class="delete-btn" @click="handleDelete(video)">
              <text class="delete-text">删除</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-state">
        <text class="empty-icon">🎬</text>
        <text class="empty-text">暂无上传视频</text>
      </view>
    </view>
  </view>
</template>

<script>
import { uploadVideo, getMyVideos, deleteVideo, BASE_URL } from '@/api/video'

export default {
  data() {
    return {
      BASE_URL: BASE_URL,
      title: '',
      description: '',
      videoFile: null,
      videoPath: '',
      uploading: false,
      uploadProgress: 0,
      myVideos: [],
      loading: false,
      page: 1,
      size: 10,
      total: 0
    }
  },

  onShow() {
    this.loadMyVideos()
  },

  methods: {
    chooseVideo() {
      uni.chooseVideo({
        sourceType: ['album', 'camera'],
        maxDuration: 300,
        success: (res) => {
          this.videoPath = res.tempFilePath
          this.videoFile = res
        },
        fail: () => {}
      })
    },

    removeVideo() {
      this.videoPath = ''
      this.videoFile = null
    },

    async handleUpload() {
      if (!this.videoPath) {
        uni.showToast({ title: '请选择视频', icon: 'none' })
        return
      }
      if (!this.title.trim()) {
        uni.showToast({ title: '请输入视频标题', icon: 'none' })
        return
      }

      this.uploading = true
      this.uploadProgress = 0

      // Simulate progress
      const progressTimer = setInterval(() => {
        if (this.uploadProgress < 90) {
          this.uploadProgress += 10
        }
      }, 500)

      try {
        await uploadVideo(this.videoPath, this.title.trim(), this.description.trim())
        clearInterval(progressTimer)
        this.uploadProgress = 100

        uni.showToast({ title: '上传成功', icon: 'success' })

        // Reset form
        this.title = ''
        this.description = ''
        this.videoPath = ''
        this.videoFile = null

        // Reload list
        this.page = 1
        this.loadMyVideos()
      } catch (error) {
        clearInterval(progressTimer)
        console.error('上传失败:', error)
      } finally {
        this.uploading = false
        this.uploadProgress = 0
      }
    },

    async loadMyVideos() {
      this.loading = true
      try {
        const res = await getMyVideos({ current: this.page, size: this.size })
        if (res.code === 200 || res.code === '00000') {
          this.myVideos = res.data.records || []
          this.total = res.data.total || 0
        }
      } catch (error) {
        console.error('加载视频列表失败:', error)
      } finally {
        this.loading = false
      }
    },

    async handleDelete(video) {
      uni.showModal({
        title: '确认删除',
        content: `确定要删除视频"${video.title}"吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              const result = await deleteVideo(video.id)
              if (result.code === 200 || result.code === '00000') {
                uni.showToast({ title: '删除成功', icon: 'success' })
                this.page = 1
                this.loadMyVideos()
              }
            } catch (error) {
              console.error('删除失败:', error)
            }
          }
        }
      })
    },

    getStatusText(status) {
      const map = {
        'PENDING': '审核中',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'PROCESSING': '处理中'
      }
      return map[status] || status || '审核中'
    },

    getStatusClass(status) {
      const map = {
        'PENDING': 'status-pending',
        'APPROVED': 'status-approved',
        'REJECTED': 'status-rejected',
        'PROCESSING': 'status-processing'
      }
      return map[status] || 'status-pending'
    },

    formatTime(time) {
      if (!time) return ''
      return time.substring(0, 16).replace('T', ' ')
    }
  }
}
</script>

<style lang="scss" scoped>
.upload-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 24rpx 32rpx 100rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: #333333;
  margin-bottom: 24rpx;
}

.upload-section {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300rpx;
  border: 2rpx dashed #c0c4cc;
  border-radius: 16rpx;
  background: #f9fafb;
  margin-bottom: 24rpx;

  .upload-icon {
    font-size: 80rpx;
    margin-bottom: 16rpx;
  }

  .upload-text {
    font-size: 28rpx;
    color: #333333;
    margin-bottom: 8rpx;
  }

  .upload-hint {
    font-size: 22rpx;
    color: #999999;
  }
}

.video-preview {
  position: relative;
  margin-bottom: 24rpx;
  border-radius: 16rpx;
  overflow: hidden;

  .preview-video {
    width: 100%;
    height: 400rpx;
  }

  .remove-btn {
    position: absolute;
    top: 16rpx;
    right: 16rpx;
    background: rgba(0, 0, 0, 0.6);
    padding: 8rpx 20rpx;
    border-radius: 8rpx;

    .remove-text {
      font-size: 24rpx;
      color: #ffffff;
    }
  }
}

.form-group {
  margin-bottom: 24rpx;

  .form-label {
    display: block;
    font-size: 26rpx;
    color: #333333;
    margin-bottom: 12rpx;

    .required {
      color: #ef4444;
    }
  }

  .form-input {
    width: 100%;
    height: 80rpx;
    background: #f9fafb;
    border: 2rpx solid #e5e7eb;
    border-radius: 12rpx;
    padding: 0 20rpx;
    font-size: 28rpx;
    box-sizing: border-box;
  }

  .form-textarea {
    width: 100%;
    height: 160rpx;
    background: #f9fafb;
    border: 2rpx solid #e5e7eb;
    border-radius: 12rpx;
    padding: 16rpx 20rpx;
    font-size: 28rpx;
    box-sizing: border-box;
  }
}

.progress-bar {
  margin-bottom: 24rpx;

  .progress-track {
    width: 100%;
    height: 12rpx;
    background: #e5e7eb;
    border-radius: 6rpx;
    overflow: hidden;
    margin-bottom: 8rpx;

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #1d4ed8, #3b82f6);
      border-radius: 6rpx;
      transition: width 0.3s;
    }
  }

  .progress-text {
    font-size: 22rpx;
    color: #1d4ed8;
  }
}

.upload-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: bold;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;

  &.disabled {
    opacity: 0.6;
  }
}

.my-videos-section {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);
}

.video-list {
  .video-card {
    background: #f9fafb;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 16rpx;

    &:last-child {
      margin-bottom: 0;
    }

    .video-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12rpx;

      .video-card-title {
        font-size: 28rpx;
        font-weight: bold;
        color: #333333;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        margin-right: 16rpx;
      }

      .status-badge {
        padding: 4rpx 16rpx;
        border-radius: 12rpx;
        flex-shrink: 0;

        text {
          font-size: 22rpx;
        }

        &.status-pending {
          background: #FFF3E0;
          text { color: #f59e0b; }
        }

        &.status-approved {
          background: #d1fae5;
          text { color: #059669; }
        }

        &.status-rejected {
          background: #fee2e2;
          text { color: #ef4444; }
        }

        &.status-processing {
          background: #E3F2FD;
          text { color: #1d4ed8; }
        }
      }
    }

    .video-card-desc {
      display: block;
      font-size: 24rpx;
      color: #666666;
      margin-bottom: 12rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .video-card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .video-card-time {
        font-size: 22rpx;
        color: #999999;
      }

      .delete-btn {
        padding: 4rpx 16rpx;
        border-radius: 8rpx;
        background: #fee2e2;

        .delete-text {
          font-size: 22rpx;
          color: #ef4444;
        }
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;

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
