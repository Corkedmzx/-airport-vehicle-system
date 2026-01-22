// 告警相关API
import request from '@/utils/request'
import type { Alert } from './types'

// 获取告警列表
export const getAlertsApi = (params?: any) => {
  return request.get('/alerts', { params })
}

// 获取告警详情
export const getAlertApi = (id: number) => {
  return request.get(`/alerts/${id}`)
}

// 创建告警
export const createAlertApi = (data: Partial<Alert>) => {
  return request.post('/alerts', data)
}

// 确认告警
export const acknowledgeAlertApi = (id: number, assignee?: string) => {
  return request.put(`/alerts/${id}/acknowledge`, null, { params: { assignee } })
}

// 解决告警
export const resolveAlertApi = (id: number, notes?: string) => {
  return request.put(`/alerts/${id}/resolve`, null, { params: { notes } })
}

// 获取告警统计
export const getAlertStatisticsApi = () => {
  return request.get('/alerts/statistics')
}

// 删除告警
export const deleteAlertApi = (id: number) => {
  return request.delete(`/alerts/${id}`)
}

// 批量删除告警
export const deleteAlertsApi = (alertIds: number[]) => {
  return request.delete('/alerts/batch', { data: alertIds })
}

// 更新告警
export const updateAlertApi = (id: number, data: Partial<Alert>) => {
  return request.put(`/alerts/${id}`, data)
}
