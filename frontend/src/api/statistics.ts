// 系统统计相关API
import request from '@/utils/request'

// 获取系统概览统计
export const getSystemOverviewApi = () => {
  return request.get('/statistics/system')
}

// 获取仪表盘数据
export const getDashboardDataApi = () => {
  return request.get('/statistics/dashboard')
}