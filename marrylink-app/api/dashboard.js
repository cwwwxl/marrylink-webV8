import { get, post } from '@/utils/request'

/**
 * 获取主持人工作台统计数据
 */
export function getHostDashboardStats() {
  return get('/dashboard/stats')
}

/**
 * 获取主持人本月订单列表
 */
export function getHostMonthOrders(params) {
  return get('/dashboard/orders', params)
}

/**
 * 获取主持人待处理问卷列表
 */
export function getHostPendingQuestionnaires(params) {
  return get('/dashboard/questionnaires', params)
}

/**
 * 获取主持人档期日历（本月）
 */
export function getHostMonthSchedule(params) {
  return get('/dashboard/schedule', params)
}

/**
 * 获取即将到来的婚礼列表
 */
export function getUpcomingWeddings(params) {
  return get('/dashboard/upcoming-weddings', params)
}

/**
 * 获取新人工作台数据（预留）
 */
export function getCoupleDashboardStats() {
  return get('/dashboard/stats')
}