import { get, post } from '@/utils/request'

/**
 * 获取未读消息列表
 */
export function getUnreadMessages() {
  return get('/message/unread')
}

/**
 * 获取未读消息数量
 */
export function getUnreadCount() {
  return get('/message/unread/count')
}

/**
 * 标记消息为已读
 */
export function markMessagesAsRead(messageIds) {
  return post('/message/mark-read', messageIds)
}

/**
 * 获取消息列表（分页）
 * @param {Object} params - { current, size, status }
 */
export function getMessageList(params) {
  return get('/message/list', params)
}