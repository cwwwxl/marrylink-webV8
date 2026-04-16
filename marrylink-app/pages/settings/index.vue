<template>
  <view class="settings-container">
    <scroll-view class="settings-scroll" scroll-y>
      <!-- 头像设置 -->
      <view class="settings-section">
        <view class="avatar-section" @click="chooseAvatar">
          <image
            class="avatar-preview"
            :src="avatarUrl"
            mode="aspectFill"
          ></image>
          <text class="avatar-tip">点击更换头像</text>
        </view>
      </view>
      
      <!-- 基本信息 -->
      <view class="settings-section">
        <view class="section-title">基本信息</view>
        
        <!-- 新人用户字段 -->
        <template v-if="userType === 'CUSTOMER'">
          <view class="form-item">
            <text class="label">新娘姓名</text>
            <input 
              class="input" 
              v-model="formData.brideName" 
              placeholder="请输入新娘姓名"
            />
          </view>
          
          <view class="form-item">
            <text class="label">新郎姓名</text>
            <input 
              class="input" 
              v-model="formData.groomName" 
              placeholder="请输入新郎姓名"
            />
          </view>
        </template>
        
        <!-- 主持人用户字段 -->
        <template v-if="userType === 'HOST'">
          <view class="form-item">
            <text class="label">真实姓名</text>
            <input 
              class="input" 
              v-model="formData.name" 
              placeholder="请输入真实姓名"
            />
          </view>
          
          <view class="form-item">
            <text class="label">艺名</text>
            <input 
              class="input" 
              v-model="formData.stageName" 
              placeholder="请输入艺名"
            />
          </view>
          
          <view class="form-item">
            <text class="label">邮箱</text>
            <input 
              class="input" 
              v-model="formData.email" 
              type="text"
              placeholder="请输入邮箱"
            />
          </view>
        </template>
        
        <view class="form-item">
          <text class="label">手机号</text>
          <input 
            class="input" 
            :value="formData.phone" 
            disabled
            placeholder="手机号不可修改"
          />
        </view>
      </view>
      
      <!-- 保存按钮 -->
      <view class="button-section">
        <button class="btn-save" @click="handleSave">保存修改</button>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import { updateUserInfo, uploadAvatar } from '@/api/user'
import { updateHostInfo, uploadHostAvatar } from '@/api/host'
import { BASE_URL } from '@/utils/request'

export default {
  data() {
    return {
      formData: {
        avatar: '',
        brideName: '',
        groomName: '',
        name: '',
        stageName: '',
        email: '',
        phone: ''
      }
    }
  },
  
  computed: {
    ...mapState('user', ['userInfo']),
    
    userType() {
      return this.userInfo?.userType || 'CUSTOMER'
    },
    
    avatarUrl() {
      if (!this.formData.avatar) {
        return '/static/default-avatar.png'
      }
      if (this.formData.avatar.startsWith('http')) {
        return this.formData.avatar
      }
      return BASE_URL + this.formData.avatar
    }
  },
  
  async onLoad() {
    await this.getUserInfo()
    this.loadUserData()
  },
  
  methods: {
    ...mapActions('user', ['getUserInfo']),
    
    loadUserData() {
      if (this.userInfo) {
        this.formData = {
          avatar: this.userInfo.avatar || '',
          brideName: this.userInfo.brideName || this.userInfo.realName || '',
          groomName: this.userInfo.groomName || '',
          name: this.userInfo.name || this.userInfo.realName || '',
          stageName: this.userInfo.stageName || '',
          email: this.userInfo.email || '',
          phone: this.userInfo.phone || ''
        }
      }
    },
    
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const tempFilePath = res.tempFilePaths[0]
          
          uni.showLoading({
            title: '上传中...'
          })
          
          try {
            const uploadFunc = this.userType === 'HOST' ? uploadHostAvatar : uploadAvatar
            const result = await uploadFunc(tempFilePath)
            
            if (result.code === 200 || result.code === '00000') {
              this.formData.avatar = result.data
              uni.showToast({
                title: '上传成功',
                icon: 'success'
              })
            }
          } catch (error) {
            uni.showToast({
              title: '上传失败',
              icon: 'none'
            })
          } finally {
            uni.hideLoading()
          }
        }
      })
    },
    
    async handleSave() {
      // 验证必填字段
      if (this.userType === 'CUSTOMER') {
        if (!this.formData.brideName || !this.formData.groomName) {
          uni.showToast({
            title: '请填写完整信息',
            icon: 'none'
          })
          return
        }
      } else if (this.userType === 'HOST') {
        if (!this.formData.name) {
          uni.showToast({
            title: '请填写真实姓名',
            icon: 'none'
          })
          return
        }
      }
      
      uni.showLoading({
        title: '保存中...'
      })
      
      try {
        let result
        if (this.userType === 'HOST') {
          result = await updateHostInfo({
            name: this.formData.name,
            stageName: this.formData.stageName,
            email: this.formData.email,
            avatar: this.formData.avatar
          })
        } else {
          result = await updateUserInfo({
            brideName: this.formData.brideName,
            groomName: this.formData.groomName,
            avatar: this.formData.avatar
          })
        }
        
        if (result.code === 200 || result.code === '00000') {
          await this.getUserInfo()
          uni.showToast({
            title: '保存成功',
            icon: 'success'
          })
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        }
      } catch (error) {
        uni.showToast({
          title: '保存失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.settings-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.settings-scroll {
  height: 100vh;
}

.settings-section {
  background-color: #ffffff;
  margin-bottom: 20rpx;
  
  .section-title {
    padding: 32rpx;
    font-size: 32rpx;
    font-weight: bold;
    color: #333333;
    border-bottom: 1rpx solid #f5f7fa;
  }
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 32rpx;
  
  .avatar-preview {
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    border: 4rpx solid #1d4ed8;
    margin-bottom: 24rpx;
  }
  
  .avatar-tip {
    font-size: 26rpx;
    color: #1d4ed8;
  }
}

.form-item {
  display: flex;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #f5f7fa;
  
  &:last-child {
    border-bottom: none;
  }
  
  .label {
    width: 180rpx;
    font-size: 28rpx;
    color: #333333;
  }
  
  .input {
    flex: 1;
    font-size: 28rpx;
    color: #333333;
    
    &[disabled] {
      color: #999999;
    }
  }
}

.button-section {
  padding: 40rpx 32rpx;
  
  .btn-save {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
    color: #ffffff;
    border-radius: 44rpx;
    font-size: 32rpx;
    font-weight: bold;
    border: none;
    
    &::after {
      border: none;
    }
  }
}
</style>