// 系统配置相关API
import request from '@/utils/request'

// 获取所有系统配置
export const getSystemConfigsApi = () => {
  return request.get('/system/config')
}

// 获取指定配置
export const getSystemConfigApi = (key: string) => {
  return request.get(`/system/config/${key}`)
}

// 保存系统配置
export const saveSystemConfigsApi = (configs: Record<string, string>) => {
  return request.post('/system/config', configs)
}

// 更新指定配置
export const updateSystemConfigApi = (key: string, value: string) => {
  return request.put(`/system/config/${key}`, { value })
}

