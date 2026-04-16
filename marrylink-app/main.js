import Vue from 'vue'
import App from './App'
import store from './store'

Vue.config.productionTip = false

App.mpType = 'app'

// 全局混入
Vue.mixin({
  onShow() {
    // 检查登录状态
    const token = uni.getStorageSync('token')
    const currentRoute = this.$mp && this.$mp.page ? this.$mp.page.route : ''
    
    // 白名单：不需要登录就能访问的页面
    const whiteList = [
      'pages/login/index',
      'pages/register/index',
      'pages/host/index',
      'pages/host/detail',
      'pages/index/index'
    ]
    
    if (!token && currentRoute && !whiteList.includes(currentRoute)) {
      // 如果未登录且不在白名单页面，显示提示后跳转到登录页
      uni.showLoading({
        title: '登录中...',
        mask: true
      })
      setTimeout(() => {
        uni.hideLoading()
        uni.reLaunch({
          url: '/pages/login/index'
        })
      }, 1000)
    }
  }
})

const app = new Vue({
  store,
  ...App
})
app.$mount()