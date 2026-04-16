// API 基础配置
// 开发环境使用 localhost，生产环境使用相对路径（通过 nginx 代理）
const getBaseUrl = () => {
  // 判断是否为开发环境
  const isDev = process.env.NODE_ENV === 'development'
  
  if (isDev) {
    // 开发环境：使用本地后端地址
    return 'http://localhost:8080/api/v1'
  } else {
    // 生产环境：使用相对路径，由 nginx 代理到后端
    return '/app-api'
  }
}

export const BASE_URL = getBaseUrl()
const TIMEOUT = 10000

/**
 * 封装的请求方法
 */
function request(options) {
  return new Promise((resolve, reject) => {
    // 获取token
    const token = uni.getStorageSync('token')
    
    // 显示加载
    if (options.loading !== false) {
      uni.showLoading({
        title: '加载中...',
        mask: true
      })
    }
    
    // 发起请求
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      timeout: options.timeout || TIMEOUT,
      success: (res) => {
        // 隐藏加载
        if (options.loading !== false) {
          uni.hideLoading()
        }
        
        // 处理响应
        if (res.statusCode === 200) {
          const data = res.data

          
          // 业务成功 (支持 "00000" 和 200 两种成功码)
          if (data.code === 200 || data.code === '00000') {
            resolve(data)
          }
          // token过期或未认证 (支持数字401和字符串"401")
          else if (data.code === 401 || data.code === '401') {
            uni.showToast({
              title: data.message || '未认证，请先登录',
              icon: 'none'
            })
            // 清除token
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            // 跳转到登录页
            setTimeout(() => {
              uni.reLaunch({
                url: '/pages/login/index'
              })
            }, 1500)
            reject(data)
          }
          // 其他业务错误
          else {
            uni.showToast({
              title: data.message || '请求失败',
              icon: 'none'
            })
            reject(data)
          }
        } else {
          uni.showToast({
            title: '网络请求失败',
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        // 隐藏加载
        if (options.loading !== false) {
          uni.hideLoading()
        }
        
        uni.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

/**
 * GET 请求
 */
export function get(url, data = {}, options = {}) {
  return request({
    url,
    method: 'GET',
    data,
    ...options
  })
}

/**
 * POST 请求
 */
export function post(url, data = {}, options = {}) {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

/**
 * PUT 请求
 */
export function put(url, data = {}, options = {}) {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

/**
 * DELETE 请求
 */
export function del(url, data = {}, options = {}) {
  return request({
    url,
    method: 'DELETE',
    data,
    ...options
  })
}

export default request