// 统计相关API
import request from '@/utils/request'

// 获取实时监控统计数据
export const getMonitoringStatsApi = () => {
  return request.get('/statistics/monitoring')
}

// 获取调度中心统计数据
export const getDispatchStatsApi = () => {
  return request.get('/statistics/dispatch')
}

// 获取车辆使用情况排行
export const getVehicleUsageRankingApi = () => {
  return request.get('/statistics/vehicle-usage')
}

// 获取任务效率统计
export const getTaskEfficiencyApi = () => {
  return request.get('/statistics/task-efficiency')
}
