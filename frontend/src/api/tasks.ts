// 任务相关API
import request from '@/utils/request'
import type { DispatchTask, TaskStatistics } from './types'

// 获取任务列表
export const getTasksApi = (params?: any) => {
  return request.get<DispatchTask[]>('/tasks', { params })
}

// 获取任务详情
export const getTaskApi = (id: number) => {
  return request.get<DispatchTask>(`/tasks/${id}`)
}

// 创建任务
export const createTaskApi = (data: Partial<DispatchTask>) => {
  return request.post<DispatchTask>('/tasks', data)
}

// 更新任务
export const updateTaskApi = (id: number, data: Partial<DispatchTask>) => {
  return request.put<DispatchTask>(`/tasks/${id}`, data)
}

// 删除任务
export const deleteTaskApi = (id: number) => {
  return request.delete(`/tasks/${id}`)
}

// 分配任务
export const assignTaskApi = (id: number, vehicleId: number, driverId?: number) => {
  return request.put<DispatchTask>(`/tasks/${id}/assign`, null, {
    params: { vehicleId, driverId }
  })
}

// 取消分配任务
export const unassignTaskApi = (id: number) => {
  return request.put<DispatchTask>(`/tasks/${id}/unassign`)
}

// 开始任务
export const startTaskApi = (id: number) => {
  return request.put<DispatchTask>(`/tasks/${id}/start`)
}

// 完成任务
export const completeTaskApi = (id: number) => {
  return request.put<DispatchTask>(`/tasks/${id}/complete`)
}

// 取消任务
export const cancelTaskApi = (id: number, reason?: string) => {
  return request.put<DispatchTask>(`/tasks/${id}/cancel`, null, {
    params: { reason }
  })
}

// 获取待分配任务
export const getPendingTasksApi = () => {
  return request.get<DispatchTask[]>('/tasks/pending')
}

// 获取进行中任务
export const getInProgressTasksApi = () => {
  return request.get<DispatchTask[]>('/tasks/in-progress')
}

// 根据时间范围获取任务
export const getTasksByTimeRangeApi = (startTime: string, endTime: string) => {
  return request.get<DispatchTask[]>('/tasks/by-time-range', {
    params: { startTime, endTime }
  })
}

// 获取任务统计
export const getTaskStatisticsApi = () => {
  return request.get<TaskStatistics>('/tasks/statistics')
}

// 重新发送任务（复制已完成任务）
export const resendTaskApi = (id: number) => {
  return request.post<DispatchTask>(`/tasks/${id}/resend`)
}