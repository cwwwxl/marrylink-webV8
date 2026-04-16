import { get, post, BASE_URL } from '@/utils/request'

/**
 * 获取主持人列表
 */
export function getHostList(params) {
  return get('/host/page', params)
}

/**
 * 获取主持人详情
 */
export function getHostDetail(id) {
  return get(`/host/${id}`)
}

/**
 * 搜索主持人
 */
export function searchHost(params) {
  return get('/host/search', params)
}

/**
 * 根据标签筛选主持人
 */
export function filterHostByTags(params) {
  return get('/host/filterByTags', params)
}

/**
 * 获取主持人档期
 */
export function getHostSchedule(params) {
  return get(`/order/schedule`, params)
}

/**
 * 预约主持人
 */
export function bookHost(data) {
  return post('/order/create', data)
}

/**
 * 获取标签列表
 */
export function getTagList(categoryCode) {
  return get('/tag/list', { categoryCode })
}

/**
 * 获取主持人信息
 */
export function getHostInfo() {
  return get('/host/info')
}

/**
 * 更新主持人信息
 */
export function updateHostInfo(data) {
  return post('/host/update', data)
}

/**
 * 上传主持人头像
 */
export function uploadHostAvatar(filePath) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    uni.uploadFile({
      url: BASE_URL + '/host/uploadAvatar',
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

/**
 * 获取月度订单统计
 */
export function getMonthlyOrders() {
  return get('/host/monthlyOrders')
}

/**
 * 获取主持人评价列表（从已完成订单中获取）
 * @param {number} hostId 主持人ID
 */
export function getHostReviews(hostId) {
  return get(`/host/${hostId}/reviews`)
}