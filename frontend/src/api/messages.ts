// 站内信相关API
import request from '@/utils/request'
import type { Message } from './types'

// 获取用户消息列表（分页）
export const getMessagesApi = (params?: {
  page?: number
  size?: number
  read?: boolean
  messageType?: string
}) => {
  return request.get('/messages', { params })
}

// 获取未读消息数量
export const getUnreadCountApi = () => {
  return request.get('/messages/unread-count')
}

// 标记消息为已读
export const markMessagesAsReadApi = (messageIds: number[]) => {
  return request.put('/messages/mark-read', messageIds)
}

// 标记所有消息为已读
export const markAllMessagesAsReadApi = () => {
  return request.put('/messages/mark-all-read')
}

// 删除消息
export const deleteMessageApi = (id: number) => {
  return request.delete(`/messages/${id}`)
}

// 删除所有已读消息
export const deleteAllReadMessagesApi = () => {
  return request.delete('/messages/read-all')
}

// 批量删除消息
export const deleteMessagesApi = (messageIds: number[]) => {
  return request.delete('/messages/batch', { data: messageIds })
}
