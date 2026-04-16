import { get, post, BASE_URL } from '@/utils/request'

/**
 * 用户登录（适配新权限系统）
 * 支持新人（CUSTOMER）和主持人（HOST）登录
 */
export function login(data) {
  return post('/auth/login', {
    accountId: data.phone,
    password: data.password,
    userType: data.userType || 'CUSTOMER'  // 默认为CUSTOMER，支持传入HOST
  })
}

/**
 * 用户注册（适配新权限系统）
 */
export function register(data) {
  return post('/auth/register', {
    phone: data.phone,
    name: data.name,  // 支持"新娘&新郎"格式
    password: data.password
  })
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return get('/user/info')
}

/**
 * 更新用户信息
 */
export function updateUserInfo(data) {
  return post('/user/update', data)
}

/**
 * 修改密码
 */
export function changePassword(data) {
  return post('/user/changePassword', data)
}

/**
 * 上传头像
 */
export function uploadAvatar(filePath) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    uni.uploadFile({
      url: BASE_URL + '/user/uploadAvatar',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (data.code === 200 || data.code === '00000') {
          resolve(data)
        } else {
          uni.showToast({
            title: data.message || '上传失败',
            icon: 'none'
          })
          reject(data)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '上传失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}