<template>
  <view class="login-container">
    <view class="login-header">
      <image class="logo" src="/static/logo.png" mode="aspectFit"></image>
      <text class="title">MarryLink</text>
      <text class="subtitle">让每一场婚礼都完美呈现</text>
    </view>
    
    <view class="login-form">
      <!-- 用户类型选择 -->
      <view class="user-type-tabs">
        <view
          class="tab-item"
          :class="{ active: loginForm.userType === 'CUSTOMER' }"
          @click="switchUserType('CUSTOMER')"
        >
          <text class="tab-icon">💑</text>
          <text class="tab-text">我是新人</text>
        </view>
        <view
          class="tab-item"
          :class="{ active: loginForm.userType === 'HOST' }"
          @click="switchUserType('HOST')"
        >
          <text class="tab-icon">🎤</text>
          <text class="tab-text">我是主持人</text>
        </view>
      </view>
      
      <view class="form-item">
        <view class="form-label">
          <text class="icon">📱</text>
          <text>手机号</text>
        </view>
        <input
          class="form-input"
          type="number"
          v-model="loginForm.phone"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>
      
      <view class="form-item">
        <view class="form-label">
          <text class="icon">🔒</text>
          <text>密码</text>
        </view>
        <input 
          class="form-input" 
          type="password" 
          v-model="loginForm.password" 
          placeholder="请输入密码"
        />
      </view>
      
      <!-- <view class="form-actions">
        <text class="forgot-password" @click="handleForgotPassword">忘记密码？</text>
      </view> -->
      
      <button class="btn-login" @click="handleLogin" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
      
      <view class="register-link">
        <text>还没有账号？</text>
        <text class="link" @click="goToRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  data() {
    return {
      loginForm: {
        phone: '',
        password: '',
        userType: 'CUSTOMER'  // 默认为新人
      },
      loading: false
    }
  },
  
  methods: {
    ...mapActions('user', ['login']),
    
    // 切换用户类型
    switchUserType(type) {
      this.loginForm.userType = type
    },
    
    // 验证表单
    validateForm() {
      if (!this.loginForm.phone) {
        uni.showToast({
          title: '请输入手机号',
          icon: 'none'
        })
        return false
      }
      
      if (!/^1[3-9]\d{9}$/.test(this.loginForm.phone)) {
        uni.showToast({
          title: '手机号格式不正确',
          icon: 'none'
        })
        return false
      }
      
      if (!this.loginForm.password) {
        uni.showToast({
          title: '请输入密码',
          icon: 'none'
        })
        return false
      }
      
      if (this.loginForm.password.length < 6) {
        uni.showToast({
          title: '密码长度不能少于6位',
          icon: 'none'
        })
        return false
      }
      
      return true
    },
    
    // 登录
    async handleLogin() {
      if (!this.validateForm()) {
        return
      }
      
      this.loading = true
      
      try {
        const res = await this.login(this.loginForm)
        
        uni.showToast({
          title: '登录成功',
          icon: 'success'
        })
        
        // 根据用户类型跳转到不同页面
        setTimeout(() => {
          const userType = res.data.userType
          
          if (userType === 'HOST') {
            // 主持人跳转到首页（工作台）
            uni.switchTab({
              url: '/pages/index/index'
            })
          } else if (userType === 'CUSTOMER') {
            // 新人跳转到主持人列表页
            uni.switchTab({
              url: '/pages/host/index'
            })
          } else {
            // 默认跳转到首页
            uni.switchTab({
              url: '/pages/index/index'
            })
          }
        }, 1500)
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 跳转到注册页
    goToRegister() {
      uni.navigateTo({
        url: '/pages/register/index'
      })
    },
    
    // 忘记密码
    handleForgotPassword() {
      uni.showToast({
        title: '请联系管理员重置密码',
        icon: 'none'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  padding: 80rpx 60rpx;
}

.login-header {
  text-align: center;
  margin-bottom: 100rpx;
  
  .logo {
    width: 160rpx;
    height: 160rpx;
    margin-bottom: 32rpx;
  }
  
  .title {
    display: block;
    font-size: 48rpx;
    font-weight: bold;
    color: #ffffff;
    margin-bottom: 16rpx;
  }
  
  .subtitle {
    display: block;
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.login-form {
  background-color: #ffffff;
  border-radius: 32rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
  
  .user-type-tabs {
    display: flex;
    gap: 16rpx;
    margin-bottom: 40rpx;
    
    .tab-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24rpx 0;
      background-color: #f5f7fa;
      border-radius: 16rpx;
      border: 2rpx solid transparent;
      transition: all 0.3s;
      
      .tab-icon {
        font-size: 40rpx;
        margin-bottom: 8rpx;
      }
      
      .tab-text {
        font-size: 26rpx;
        color: #666666;
      }
      
      &.active {
        background-color: rgba(29, 78, 216, 0.1);
        border-color: #1d4ed8;
        
        .tab-text {
          color: #1d4ed8;
          font-weight: bold;
        }
      }
    }
  }
  
  .form-item {
    margin-bottom: 40rpx;
    
    .form-label {
      display: flex;
      align-items: center;
      margin-bottom: 16rpx;
      font-size: 28rpx;
      color: #333333;
      
      .icon {
        margin-right: 8rpx;
        font-size: 32rpx;
      }
    }
    
    .form-input {
      width: 100%;
      height: 88rpx;
      padding: 0 24rpx;
      background-color: #f5f7fa;
      border-radius: 16rpx;
      font-size: 28rpx;
      color: #333333;
    }
  }
  
  .form-actions {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 40rpx;
    
    .forgot-password {
      font-size: 24rpx;
      color: #1d4ed8;
    }
  }
  
  .btn-login {
    width: 100%;
    height: 88rpx;
    background-color: #1d4ed8;
    color: #ffffff;
    border: none;
    border-radius: 16rpx;
    font-size: 32rpx;
    font-weight: bold;
    line-height: 88rpx;
    
    &:disabled {
      opacity: 0.6;
    }
  }
  
  .register-link {
    text-align: center;
    margin-top: 32rpx;
    font-size: 28rpx;
    color: #666666;
    
    .link {
      color: #1d4ed8;
      margin-left: 8rpx;
    }
  }
}
</style>