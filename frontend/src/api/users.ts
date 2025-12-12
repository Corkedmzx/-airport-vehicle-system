// 用户相关API
import request from '@/utils/request'
import type { User } from './types'

// 获取用户列表
export const getUsersApi = (params?: any) => {
  return request.get('/users', { params })
}

// 获取用户详情
export const getUserApi = (id: number) => {
  return request.get(`/users/${id}`)
}

// 创建用户
export const createUserApi = (data: Partial<User>) => {
  return request.post('/users', data)
}

// 更新用户
export const updateUserApi = (id: number, data: Partial<User>) => {
  return request.put(`/users/${id}`, data)
}

// 删除用户
export const deleteUserApi = (id: number) => {
  return request.delete(`/users/${id}`)
}

// 更新用户状态
export const updateUserStatusApi = (id: number, status: number) => {
  return request.put(`/users/${id}/status`, null, { params: { status } })
}

// 重置密码
export const resetPasswordApi = (id: number, newPassword: string) => {
  return request.put(`/users/${id}/password`, null, { params: { newPassword } })
}

// 获取用户统计
export const getUserStatisticsApi = () => {
  return request.get('/users/statistics')
}

