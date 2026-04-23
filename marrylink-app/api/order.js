import { get, post, put } from '@/utils/request'

/**
 * 获取订单列表（分页）
 */
export function getOrderList(params) {
  return get('/order/page', params)
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id) {
  return get(`/order/${id}`)
}

/**
 * 创建订单
 */
export function createOrder(data) {
  return post('/order/create', data)
}

/**
 * 更新订单状态
 */
export function updateOrderStatus(id, status) {
  return put(`/order/${id}/status?status=${status}`)
}

/**
 * 对已完成订单进行评分和评价
 * @param {number} id 订单ID
 * @param {number} rating 评分 1-5
 * @param {string} comment 评价内容（可选）
 */
export function rateOrder(id, rating, comment) {
  return post(`/order/${id}/rate`, { rating, comment })
}

// ==================== 佣金管理 ====================
/**
 * 获取佣金订单列表（主持人查看自己的）
 */
export function getCommissionList(params) {
  return get('/commission/page', params)
}

/**
 * 主持人支付佣金
 */
export function payCommission(id) {
  return post(`/commission/pay/${id}`)
}

// ==================== 主持人钱包 ====================
/**
 * 获取我的钱包
 */
export function getMyWallet() {
  return get('/host-wallet/my')
}

/**
 * 提现申请
 */
export function submitWithdrawal(data) {
  return post('/host-wallet/withdraw', data)
}

/**
 * 获取提现记录
 */
export function getWithdrawalList(params) {
  return get('/host-wallet/withdrawal/page', params)
}