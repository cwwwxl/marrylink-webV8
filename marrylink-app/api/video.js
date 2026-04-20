import { get, post, BASE_URL } from '@/utils/request'

/**
 * 获取首页展示视频列表
 */
export function getHomeVideos() {
  return get('/host-video/home-list')
}

/**
 * 获取视频详情
 */
export function getVideoDetail(id) {
  return get(`/host-video/${id}`)
}

/**
 * 获取主持人自己的视频列表
 */
export function getMyVideos(params) {
  return get('/host-video/page', params)
}

/**
 * 上传案例视频
 */
export function uploadVideo(filePath, title, description) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')

    uni.uploadFile({
      url: BASE_URL + '/host-video/upload',
      filePath: filePath,
      name: 'file',
      formData: {
        title: title || '',
        description: description || ''
      },
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
 * 更新视频信息
 */
export function updateVideo(data) {
  return post('/host-video/update', data)
}

/**
 * 删除视频
 */
export function deleteVideo(id) {
  return get(`/host-video/${id}/delete`)
}

export { BASE_URL }
