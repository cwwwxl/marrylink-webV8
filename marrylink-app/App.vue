<script>
export default {
  onLaunch: function() {
    console.log('App Launch')
    // 初始化应用
    this.initApp()
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  },
  methods: {
    initApp() {
      // 检查是否有缓存的token
      const token = uni.getStorageSync('token')
      if (token) {
        // 验证token是否有效
        this.$store.dispatch('user/checkToken').catch(() => {
          // token无效，已经在action中清除了用户信息
          // 不需要强制跳转，由页面的onShow生命周期决定是否跳转
        })
      }
    }
  }
}
</script>

<style lang="scss">
/* 全局样式 */
@import '@/styles/common.scss';

page {
  background-color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

/* 主题色变量 */
:root {
  --primary-color: #1d4ed8;
  --bg-color: #ffffff;
  --text-color: #333333;
  --text-secondary: #666666;
  --border-color: #e5e7eb;
}
</style>