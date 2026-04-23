import request from '@/utils/request'

// 通用请求方法
export function get(url, params) {
  return request({
    url,
    method: 'get',
    params
  })
}

export function post(url, data) {
  return request({
    url,
    method: 'post',
    data
  })
}

export function put(url, data, config) {
  return request({
    url,
    method: 'put',
    data,
    ...config
  })
}

export function del(url, params) {
  return request({
    url,
    method: 'delete',
    params
  })
}

// 控制台统计
export const getDashboardStats = () => get('/dashboard/admin/stats')
export const getMonthlyOrderTrend = (year) => get('/dashboard/order-trend/monthly', { year })
export const getYearlyOrderTrend = () => get('/dashboard/order-trend/yearly')
export const getOrderStatusDistribution = () => get('/dashboard/order-status-distribution')
export const getRecentOrders = () => get('/dashboard/recent-orders')
export const getHostDashboardStats = () => get('/dashboard/host/stats')

// 主持人管理
export const getHostPage = (params) => get('/host/page', params)
export const getHostById = (id) => get(`/host/${id}`)
export const saveHost = (data) => post('/host', data)
export const updateHost = (data) => put('/host', data)
export const deleteHost = (id) => del(`/host/${id}`)
export const updateHostStatus = (id, status) => put(`/host/${id}/status`, null, { params: { status } })

// 主持人导入导出
export const importHost = (formData) => {
  return request({
    url: '/host/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
export const downloadHostTemplate = () => {
  return request({
    url: '/host/template',
    method: 'get',
    responseType: 'blob'
  })
}

// 用户管理
export const getUserPage = (params) => get('/user/page', params)
export const getUserById = (id) => get(`/user/${id}`)
export const updateUserStatus = (id, status) => put(`/user/${id}/status`, null, { params: { status } })

// 订单管理
export const getOrderPage = (params) => get('/order/page', params)
export const getOrderById = (id) => get(`/order/${id}`)
export const saveOrder = (data) => post('/order', data)
export const updateOrder = (data) => put('/order', data)
export const deleteOrder = (id) => del(`/order/${id}`)
export const updateOrderStatus = (id, status) => {
  return request({
    url: `/order/${id}/status`,
    method: 'put',
    params: { status }
  })
}
export const getOrderSchedule = (params) => get('/order/mySchedule', params)

// 订单日志管理
export const getOrderLogPage = (params) => get('/order/log/page', params)

// 标签分类管理
export const getTagCategoryPage = (params) => get('/tag-category/page', params)
export const getTagCategoryList = () => get('/tag-category/list')
export const getTagCategoryById = (id) => get(`/tag-category/${id}`)
export const saveTagCategory = (data) => post('/tag-category', data)
export const updateTagCategory = (data) => put('/tag-category', data)
export const deleteTagCategory = (id) => del(`/tag-category/${id}`)
export const updateTagCategoryStatus = (id, status) => put(`/tag-category/${id}/status`, null, { params: { status } })

// 标签管理
export const getTagPage = (params) => get('/tag/page', params)
export const getTagList = (categoryCode) => get('/tag/list', { categoryCode })
export const getTagById = (id) => get(`/tag/${id}`)
export const saveTag = (data) => post('/tag', data)
export const updateTag = (data) => put('/tag', data)
export const deleteTag = (id) => del(`/tag/${id}`)
export const updateTagStatus = (id, status) => put(`/tag/${id}/status`, null, { params: { status } })

// 问卷模板管理
export const getQuestionnaireTemplatePage = (params) => get('/questionnaire-template/page', params)
export const getQuestionnaireTemplateList = () => get('/questionnaire-template/list')
export const getQuestionnaireTemplateById = (id) => get(`/questionnaire-template/${id}`)
export const saveQuestionnaireTemplate = (data) => post('/questionnaire-template', data)
export const updateQuestionnaireTemplate = (data) => put('/questionnaire-template', data)
export const deleteQuestionnaireTemplate = (id) => del(`/questionnaire-template/${id}`)
export const updateQuestionnaireTemplateStatus = (id, status) => put(`/questionnaire-template/${id}/status`, null, { params: { status } })

// 问卷提交记录管理
export const getQuestionnaireSubmissionPage = (params) => get('/questionnaire-submission/page', params)
export const getQuestionnaireSubmissionById = (id) => get(`/questionnaire-submission/${id}`)
export const updateQuestionnaireSubmissionStatus = (id, status) => {
  return request({
    url: `/questionnaire-submission/${id}/status`,
    method: 'put',
    params: { status }
  })
}
export const exportQuestionnaireSubmissionPdf = (id) => {
  return request({
    url: `/questionnaire-submission/${id}/export-pdf`,
    method: 'get',
    responseType: 'blob'
  })
}

// 用户端问卷API
export const getUserQuestionnaireSubmissions = (params) => get(`/questionnaire-submission/userQuestion`, params)
export const submitQuestionnaire = (data) => post('/questionnaire-submission/submit', data)
export const updateQuestionnaireSubmission = (data) => put('/questionnaire-submission/update', data)

// 案例视频管理
export const getHostVideoPage = (params) => get('/host-video/page', params)
export const getHostVideoById = (id) => get(`/host-video/${id}`)
export const uploadHostVideo = (formData) => {
  return request({
    url: '/host-video/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 300000
  })
}
export const updateHostVideo = (data) => put('/host-video', data)
export const deleteHostVideo = (id) => del(`/host-video/${id}`)
export const updateHostVideoShowOnHome = (id, showOnHome) => put(`/host-video/${id}/showOnHome`, null, { params: { showOnHome } })
export const updateHostVideoStatus = (id, status) => put(`/host-video/${id}/status`, null, { params: { status } })

// ==================== 支付管理 ====================
// 用户支付订单
export function payOrder(orderId) {
  return post(`/payment/pay/${orderId}`)
}
// 托管资金列表
export function getEscrowPage(params) {
  return get('/payment/escrow/page', params)
}
// 获取订单托管信息
export function getEscrowByOrderId(orderId) {
  return get(`/payment/escrow/${orderId}`)
}

// ==================== 结算管理 ====================
// 结算订单
export function settleOrder(orderId) {
  return post(`/settlement/settle/${orderId}`)
}
// 结算列表
export function getSettlementPage(params) {
  return get('/settlement/page', params)
}
// 结算详情
export function getSettlementById(id) {
  return get(`/settlement/${id}`)
}

// ==================== 佣金管理 ====================
// 佣金订单列表
export function getCommissionPage(params) {
  return get('/commission/page', params)
}
// 佣金详情
export function getCommissionById(id) {
  return get(`/commission/${id}`)
}
// 主持人支付佣金
export function payCommission(id) {
  return post(`/commission/pay/${id}`)
}
// 标记逾期
export function markCommissionOverdue(id) {
  return post(`/commission/mark-overdue/${id}`)
}
// 禁止主持人接单
export function banHost(hostId) {
  return post(`/commission/ban-host/${hostId}`)
}
// 恢复主持人接单
export function unbanHost(hostId) {
  return post(`/commission/unban-host/${hostId}`)
}

// ==================== 主持人钱包 ====================
// 获取自己的钱包
export function getMyWallet() {
  return get('/host-wallet/my')
}
// 管理员获取主持人钱包
export function getHostWallet(hostId) {
  return get(`/host-wallet/${hostId}`)
}
// 钱包列表
export function getHostWalletPage(params) {
  return get('/host-wallet/page', params)
}
// 提现申请
export function submitWithdrawal(data) {
  return post('/host-wallet/withdraw', data)
}
// 提现列表
export function getWithdrawalPage(params) {
  return get('/host-wallet/withdrawal/page', params)
}
// 审核提现
export function auditWithdrawal(id, data) {
  return put(`/host-wallet/withdrawal/${id}/audit`, data)
}

// ==================== 平台设置 ====================
// 获取所有设置
export function getPlatformSettings() {
  return get('/platform-settings/list')
}
// 更新设置
export function updatePlatformSetting(data) {
  return put('/platform-settings/update', data)
}

// ==================== 平台财务（佣金收入 & 提现） ====================
// 获取平台账户
export function getPlatformAccount() {
  return get('/platform-finance/account')
}
// 平台提现
export function submitPlatformWithdrawal(data) {
  return post('/platform-finance/withdraw', data)
}
// 提现记录列表
export function getPlatformWithdrawalPage(params) {
  return get('/platform-finance/withdrawal/page', params)
}
// 确认提现完成
export function completePlatformWithdrawal(id) {
  return put(`/platform-finance/withdrawal/${id}/complete`)
}
// 取消提现
export function cancelPlatformWithdrawal(id) {
  return put(`/platform-finance/withdrawal/${id}/cancel`)
}