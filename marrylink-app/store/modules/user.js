import { login, register, getUserInfo } from '@/api/user'
import { getHostInfo } from '@/api/host'

const state = {
  token: uni.getStorageSync('token') || '',
  userInfo: uni.getStorageSync('userInfo') || null
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token
    uni.setStorageSync('token', token)
  },
  SET_USER_INFO(state, userInfo) {
    state.userInfo = userInfo
    uni.setStorageSync('userInfo', userInfo)
  },
  CLEAR_USER(state) {
    state.token = ''
    state.userInfo = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }
}

const actions = {
  // 登录（适配新权限系统）
  async login({ commit }, loginForm) {
    try {
      const res = await login(loginForm)
      // 适配后端返回的 code: "00000" 或 code: 200
      if (res.code === 200 || res.code === '00000' || res.message === 'success') {
        // 新权限系统返回的数据结构
        commit('SET_TOKEN', res.data.token)
        
        // 构造用户信息对象
        const userInfo = {
          accountId: res.data.accountId,
          refId: res.data.refId,
          userType: res.data.userType,
          realName: res.data.realName,
          phone: res.data.phone,
          email: res.data.email,
          roles: res.data.roles,
          permissions: res.data.permissions
        }
        commit('SET_USER_INFO', userInfo)
        
        return Promise.resolve(res)
      } else {
        uni.showToast({
          title: res.message || '登录失败',
          icon: 'none'
        })
        return Promise.reject(res)
      }
    } catch (error) {
      uni.showToast({
        title: error.message || '登录失败',
        icon: 'none'
      })
      return Promise.reject(error)
    }
  },
  
  // 注册（适配新权限系统）
  async register({ commit }, registerForm) {
    try {
      const res = await register(registerForm)
      // 适配后端返回的 code: "00000" 或 code: 200
      if (res.code === 200 || res.code === '00000' || res.message === 'success') {
        // 注册成功后自动登录
        commit('SET_TOKEN', res.data.token)
        
        const userInfo = {
          accountId: res.data.accountId,
          refId: res.data.refId,
          userType: res.data.userType,
          realName: res.data.realName,
          phone: res.data.phone,
          email: res.data.email,
          roles: res.data.roles,
          permissions: res.data.permissions
        }
        commit('SET_USER_INFO', userInfo)
        
        return Promise.resolve(res)
      } else {
        uni.showToast({
          title: res.message || '注册失败',
          icon: 'none'
        })
        return Promise.reject(res)
      }
    } catch (error) {
      uni.showToast({
        title: error.message || '注册失败',
        icon: 'none'
      })
      return Promise.reject(error)
    }
  },
  
  // 获取用户详细信息
  async getUserInfo({ commit, state }) {
    try {
      const currentUserInfo = state.userInfo || {}
      const isHost = currentUserInfo.userType === 'HOST'
      
      // 根据用户类型调用不同接口
      const res = isHost ? await getHostInfo() : await getUserInfo()
      
      // 适配后端返回的 code: "00000" 或 code: 200
      if (res.code === 200 || res.code === '00000' || res.message === 'success') {
        // 合并用户详细信息
        const userInfo = {
          ...currentUserInfo,
          ...res.data
        }
        commit('SET_USER_INFO', userInfo)
        return Promise.resolve(res)
      } else {
        return Promise.reject(res)
      }
    } catch (error) {
      return Promise.reject(error)
    }
  },
  
  // 检查token
  async checkToken({ commit, state }) {
    if (!state.token) {
      return Promise.reject('No token')
    }
    try {
      const currentUserInfo = state.userInfo || {}
      const isHost = currentUserInfo.userType === 'HOST'
      
      // 根据用户类型调用不同接口
      const res = isHost ? await getHostInfo() : await getUserInfo()
      
      // 适配后端返回的 code: "00000" 或 code: 200
      if (res.code === 200 || res.code === '00000' || res.message === 'success') {
        const userInfo = {
          ...currentUserInfo,
          ...res.data
        }
        commit('SET_USER_INFO', userInfo)
        return Promise.resolve(res)
      } else {
        commit('CLEAR_USER')
        return Promise.reject(res)
      }
    } catch (error) {
      commit('CLEAR_USER')
      return Promise.reject(error)
    }
  },
  
  // 退出登录
  logout({ commit }) {
    commit('CLEAR_USER')
    uni.reLaunch({
      url: '/pages/login/index'
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}