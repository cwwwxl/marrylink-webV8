<template>
  <view class="register-container">
    <view class="register-header">
      <text class="title">注册账号</text>
      <text class="subtitle">加入 MarryLink，开启美好婚礼之旅</text>
    </view>
    
    <view class="register-form">
      <view class="form-item">
        <view class="form-label">
          <text class="icon">📱</text>
          <text>手机号</text>
        </view>
        <input 
          class="form-input" 
          type="number" 
          v-model="registerForm.phone" 
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>
      
      <view class="form-item">
        <view class="form-label">
          <text class="icon">👤</text>
          <text>新人姓名</text>
        </view>
        <input
          class="form-input"
          type="text"
          v-model="registerForm.name"
          placeholder="请输入新娘&新郎姓名（如：张三&李四）"
        />
        <view class="form-tip">提示：请用&符号分隔新娘和新郎姓名</view>
      </view>
      
      <view class="form-item">
        <view class="form-label">
          <text class="icon">🔒</text>
          <text>密码</text>
        </view>
        <input 
          class="form-input" 
          type="password" 
          v-model="registerForm.password" 
          placeholder="请输入密码（至少6位）"
        />
      </view>
      
      <view class="form-item">
        <view class="form-label">
          <text class="icon">🔒</text>
          <text>确认密码</text>
        </view>
        <input 
          class="form-input" 
          type="password" 
          v-model="registerForm.confirmPassword" 
          placeholder="请再次输入密码"
        />
      </view>
      
      <view class="agreement">
        <checkbox-group @change="handleAgreementChange">
          <label class="agreement-label">
            <checkbox :checked="agreed" color="#1d4ed8" />
            <text>我已阅读并同意</text>
            <text class="link" @click.stop="showAgreement">《用户协议》</text>
            <text>和</text>
            <text class="link" @click.stop="showPrivacy">《隐私政策》</text>
          </label>
        </checkbox-group>
      </view>
      
      <button class="btn-register" @click="handleRegister" :disabled="loading || !agreed">
        {{ loading ? '注册中...' : '注册' }}
      </button>
      
      <view class="login-link">
        <text>已有账号？</text>
        <text class="link" @click="goToLogin">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  data() {
    return {
      registerForm: {
        phone: '',
        name: '',
        password: '',
        confirmPassword: ''
      },
      agreed: false,
      loading: false
    }
  },
  
  methods: {
    ...mapActions('user', ['register']),
    
    // 验证表单
    validateForm() {
      if (!this.registerForm.phone) {
        uni.showToast({
          title: '请输入手机号',
          icon: 'none'
        })
        return false
      }
      
      if (!/^1[3-9]\d{9}$/.test(this.registerForm.phone)) {
        uni.showToast({
          title: '手机号格式不正确',
          icon: 'none'
        })
        return false
      }
      
      if (!this.registerForm.name) {
        uni.showToast({
          title: '请输入姓名',
          icon: 'none'
        })
        return false
      }
      
      if (!this.registerForm.password) {
        uni.showToast({
          title: '请输入密码',
          icon: 'none'
        })
        return false
      }
      
      if (this.registerForm.password.length < 6) {
        uni.showToast({
          title: '密码长度不能少于6位',
          icon: 'none'
        })
        return false
      }
      
      if (this.registerForm.password !== this.registerForm.confirmPassword) {
        uni.showToast({
          title: '两次输入的密码不一致',
          icon: 'none'
        })
        return false
      }
      
      if (!this.agreed) {
        uni.showToast({
          title: '请阅读并同意用户协议和隐私政策',
          icon: 'none'
        })
        return false
      }
      
      return true
    },
    
    // 注册
    async handleRegister() {
      if (!this.validateForm()) {
        return
      }
      
      this.loading = true
      
      try {
        const { confirmPassword, ...data } = this.registerForm
        await this.register(data)
        
        uni.showToast({
          title: '注册成功',
          icon: 'success'
        })
        
        setTimeout(() => {
          uni.switchTab({
            url: '/pages/index/index'
          })
        }, 1500)
      } catch (error) {
        console.error('注册失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 协议变更
    handleAgreementChange(e) {
      this.agreed = e.detail.value.length > 0
    },
    
    // 显示用户协议
    showAgreement() {
      uni.showModal({
        title: '用户协议',
        content: '这里是用户协议的内容...',
        showCancel: false
      })
    },
    
    // 显示隐私政策
    showPrivacy() {
      uni.showModal({
        title: '隐私政策',
        content: '这里是隐私政策的内容...',
        showCancel: false
      })
    },
    
    // 跳转到登录页
    goToLogin() {
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.register-container {
  min-height: 100vh;
  background-color: #ffffff;
  padding: 40rpx 60rpx;
}

.register-header {
  margin-bottom: 60rpx;
  
  .title {
    display: block;
    font-size: 48rpx;
    font-weight: bold;
    color: #1d4ed8;
    margin-bottom: 16rpx;
  }
  
  .subtitle {
    display: block;
    font-size: 28rpx;
    color: #666666;
  }
}

.register-form {
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
    
    .form-tip {
      margin-top: 8rpx;
      font-size: 24rpx;
      color: #999999;
    }
  }
  
  .agreement {
    margin-bottom: 40rpx;
    
    .agreement-label {
      display: flex;
      align-items: center;
      font-size: 24rpx;
      color: #666666;
      
      checkbox {
        margin-right: 8rpx;
      }
      
      .link {
        color: #1d4ed8;
        margin: 0 4rpx;
      }
    }
  }
  
  .btn-register {
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
  
  .login-link {
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