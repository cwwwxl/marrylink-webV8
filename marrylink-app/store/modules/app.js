const state = {
  loading: false,
  networkStatus: true
}

const mutations = {
  SET_LOADING(state, loading) {
    state.loading = loading
  },
  SET_NETWORK_STATUS(state, status) {
    state.networkStatus = status
  }
}

const actions = {
  showLoading({ commit }) {
    commit('SET_LOADING', true)
    uni.showLoading({
      title: '加载中...',
      mask: true
    })
  },
  hideLoading({ commit }) {
    commit('SET_LOADING', false)
    uni.hideLoading()
  },
  checkNetwork({ commit }) {
    uni.getNetworkType({
      success: (res) => {
        const networkType = res.networkType
        if (networkType === 'none') {
          commit('SET_NETWORK_STATUS', false)
          uni.showToast({
            title: '网络连接失败',
            icon: 'none'
          })
        } else {
          commit('SET_NETWORK_STATUS', true)
        }
      }
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}