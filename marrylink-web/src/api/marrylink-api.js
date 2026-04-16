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