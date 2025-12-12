// 日志相关API
import request from '@/utils/request'
import type { Log } from './types'

// 获取日志列表
export const getLogsApi = (params?: any) => {
  return request.get('/logs', { params })
}

// 获取日志详情
export const getLogApi = (id: number) => {
  return request.get(`/logs/${id}`)
}

// 删除日志
export const deleteLogApi = (id: number) => {
  return request.delete(`/logs/${id}`)
}

// 清空日志
export const clearLogsApi = () => {
  return request.delete('/logs')
}

// 获取日志统计
export const getLogStatisticsApi = () => {
  return request.get('/logs/statistics')
}

