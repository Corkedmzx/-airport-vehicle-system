// 认证相关API
import request from '@/utils/request'
import type { LoginRequest, LoginResponse } from './types'

// 登录
export const loginApi = (data: LoginRequest) => {
  return request.post<LoginResponse>('/auth/login', data)
}

// 注册
export const registerApi = (data: LoginRequest) => {
  return request.post<LoginResponse>('/auth/register', data)
}

// 登出
export const logoutApi = () => {
  return request.post('/auth/logout')
}

// 获取用户信息
export const getUserInfoApi = () => {
  return request.get('/auth/info')
}

// 账户找回
export const forgotPasswordApi = (data: { phone: string }) => {
  return request.post('/auth/forgot-password', { phone: data.phone })
}

// 设置新密码
export const resetPasswordApi = (data: { username: string; password: string }) => {
  return request.post('/auth/reset-password', data)
}