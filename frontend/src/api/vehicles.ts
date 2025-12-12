// 车辆相关API
import request from '@/utils/request'
import type { Vehicle, VehicleLocationDTO, VehicleStatistics } from './types'

// 获取车辆列表
export const getVehiclesApi = (params?: any) => {
  return request.get<Vehicle[]>('/vehicles', { params })
}

// 获取车辆详情
export const getVehicleApi = (id: number) => {
  return request.get<Vehicle>(`/vehicles/${id}`)
}

// 创建车辆
export const createVehicleApi = (data: Partial<Vehicle>) => {
  return request.post<Vehicle>('/vehicles', data)
}

// 更新车辆
export const updateVehicleApi = (id: number, data: Partial<Vehicle>) => {
  return request.put<Vehicle>(`/vehicles/${id}`, data)
}

// 删除车辆
export const deleteVehicleApi = (id: number) => {
  return request.delete(`/vehicles/${id}`)
}

// 更新车辆位置
export const updateVehicleLocationApi = (id: number, data: VehicleLocationDTO) => {
  return request.put<Vehicle>(`/vehicles/${id}/location`, data)
}

// 获取正常车辆
export const getActiveVehiclesApi = () => {
  return request.get<Vehicle[]>('/vehicles/active')
}

// 根据状态获取车辆
export const getVehiclesByStatusApi = (status: number) => {
  return request.get<Vehicle[]>(`/vehicles/by-status/${status}`)
}

// 根据类型获取车辆
export const getVehiclesByTypeApi = (typeId: number) => {
  return request.get<Vehicle[]>(`/vehicles/by-type/${typeId}`)
}

// 获取车辆统计
export const getVehicleStatisticsApi = () => {
  return request.get<VehicleStatistics>('/vehicles/statistics')
}