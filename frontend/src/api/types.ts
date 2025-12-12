// 类型定义

// 登录相关类型
export interface LoginRequest {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
}

export interface LoginResponse {
  message: string
  data: { token: any; user: any; expiresIn: any }
  code: number
  token: string
  tokenType: string
  user: {
    id: number
    username: string
    realName: string
    email: string
    avatar: string
    roles: string[]
  }
  expiresIn: number
}

// 车辆相关类型
export interface Vehicle {
  id: number
  vehicleNo: string
  vehicleTypeId: number
  brand?: string
  model?: string
  color?: string
  engineNo?: string
  vin?: string
  purchaseDate?: string
  mileage: number
  fuelCapacity?: number
  currentFuel?: number
  gpsDeviceId?: string
  status: number
  locationLongitude?: number
  locationLatitude?: number
  locationAddress?: string
  lastUpdateTime?: string
  createTime: string
  updateTime: string
}

export interface VehicleLocationDTO {
  vehicleId: number
  longitude: number
  latitude: number
  altitude?: number
  speed?: number
  direction?: number
  accuracy?: number
  locationTime: string
  address?: string
}

export interface VehicleStatistics {
  totalCount: number
  activeCount: number
  maintenanceCount: number
  faultCount: number
  offlineCount: number
}

// 任务相关类型
export interface DispatchTask {
  id: number
  taskNo: string
  taskName: string
  taskType: string
  priority: number
  description?: string
  startLocation: string
  endLocation: string
  startTime: string
  endTime?: string
  actualStartTime?: string
  actualEndTime?: string
  assignedVehicleId?: number
  assignedDriverId?: number
  status: number
  progress: number
  remark?: string
  createTime: string
  updateTime: string
}

export interface TaskStatistics {
  totalCount: number
  pendingCount: number
  assignedCount: number
  inProgressCount: number
  completedCount: number
  cancelledCount: number
  exceptionCount: number
  todayCount: number
}

// 通用响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 查询参数类型
export interface QueryParams {
  page?: number
  size?: number
  sort?: string
  order?: 'asc' | 'desc'
  [key: string]: any
}

// 告警相关类型
export interface Alert {
  id: string
  title: string
  description: string
  severity: 'high' | 'medium' | 'low'
  category: string
  vehicleId: string
  vehiclePlate: string
  status: 'unprocessed' | 'processing' | 'resolved'
  assignee?: string
  createdAt: string
  acknowledged: boolean
  read?: boolean
  resolution?: {
    assignee: string
    resolvedAt: string
    notes: string
  }
}

// 用户相关类型
export interface User {
  id: string
  username: string
  realName: string
  email: string
  phone: string
  role: string
  department: string
  status: 'active' | 'inactive' | 'locked'
  avatar?: string
  lastLoginAt?: string
  loginCount?: number
  createdAt: string
  remark?: string
}

// 日志相关类型
export interface Log {
  id: string
  timestamp: string
  level: 'ERROR' | 'WARN' | 'INFO' | 'DEBUG'
  category: 'OPERATION' | 'SYSTEM' | 'ERROR' | 'AUDIT'
  message: string
  stackTrace?: string
  user?: string
  userId?: string
  ipAddress: string
  module: string
  read?: boolean
  context?: Record<string, any>
}

// 地图车辆扩展类型
export interface MapVehicle extends Vehicle {
  plateNumber: string
  vehicleType: string
  location?: string
  latitude?: number
  longitude?: number
  speed?: number
  batteryLevel?: number
  lastUpdate: string
  distanceToTasks?: number
  currentLoad?: number
  efficiencyScore?: number
  currentTask?: {
    taskName: string
    progress: number
  }
}