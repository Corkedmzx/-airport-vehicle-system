<template>
  <div class="monitoring-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">实时监控</h1>
      <p class="page-description">实时监控车辆状态、位置和运行情况</p>
      <div class="header-actions">
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
          全屏监控
        </el-button>
      </div>
    </div>

    <!-- 监控统计 -->
    <div class="monitoring-stats">
      <div class="stat-card">
        <div class="stat-icon active">
          <el-icon><Van /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ monitoringStats.activeVehicles }}</div>
          <div class="stat-label">在线车辆</div>
          <div class="stat-trend">活跃 {{ monitoringStats.activeRate }}%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon warning">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ monitoringStats.alerts }}</div>
          <div class="stat-label">实时告警</div>
          <div class="stat-trend">待处理 {{ monitoringStats.pendingAlerts }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon success">
          <el-icon><Odometer /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ monitoringStats.runningTasks }}</div>
          <div class="stat-label">执行中任务</div>
          <div class="stat-trend">完成率 {{ monitoringStats.completionRate }}%</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon info">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ monitoringStats.systemUptime }}</div>
          <div class="stat-label">系统运行时间</div>
          <div class="stat-trend">正常</div>
        </div>
      </div>
    </div>

    <!-- 监控内容区域 -->
    <div class="monitoring-content">
      <!-- 实时车辆状态 -->
      <div class="monitoring-section">
        <div class="section-header">
          <h3 class="section-title">实时车辆状态</h3>
          <el-button type="primary" link @click="$router.push('/vehicles')">
            查看全部
          </el-button>
        </div>
        
        <div class="vehicle-grid">
          <div
            v-for="vehicle in realTimeVehicles"
            :key="vehicle.id"
            class="vehicle-card"
            :class="getVehicleStatusClass(vehicle.status)"
            @click="viewVehicleDetail(vehicle.id)"
          >
            <div class="vehicle-header">
              <div class="vehicle-info">
                <div class="plate-number">{{ vehicle.plateNumber }}</div>
                <div class="vehicle-type">{{ vehicle.vehicleType }}</div>
              </div>
              <div class="vehicle-status">
                <el-tag :type="getVehicleStatusType(vehicle.status)" size="small">
                  {{ getVehicleStatusText(vehicle.status) }}
                </el-tag>
              </div>
            </div>
            
            <div class="vehicle-details">
              <div class="detail-item">
                <span class="label">位置:</span>
                <span class="value">{{ vehicle.location || '未知' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">速度:</span>
                <span class="value">{{ vehicle.speed || 0 }} km/h</span>
              </div>
              <div class="detail-item">
                <span class="label">电量:</span>
                <span class="value">{{ vehicle.batteryLevel || 0 }}%</span>
              </div>
              <div class="detail-item">
                <span class="label">最后更新:</span>
                <span class="value">{{ formatTime(vehicle.lastUpdate) }}</span>
              </div>
            </div>
            
            <div class="vehicle-progress" v-if="vehicle.currentTask">
              <div class="progress-label">当前任务进度</div>
              <el-progress 
                :percentage="vehicle.currentTask.progress || 0" 
                :show-text="false"
                :stroke-width="6"
              />
              <div class="progress-text">{{ vehicle.currentTask.taskName }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 实时告警 -->
      <div class="monitoring-section">
        <div class="section-header">
          <h3 class="section-title">实时告警</h3>
          <el-button type="primary" link @click="$router.push('/alerts')">
            查看全部
          </el-button>
        </div>
        
        <div class="alerts-list">
          <div 
            v-for="alert in realTimeAlerts"
            :key="alert.id"
            class="alert-item"
            :class="getAlertSeverityClass(alert.severity)"
            @click="handleAlert(alert)"
          >
            <div class="alert-icon">
              <el-icon>
                <Warning v-if="alert.severity === 'high'" />
                <InfoFilled v-else-if="alert.severity === 'medium'" />
                <CircleCheck v-else />
              </el-icon>
            </div>
            <div class="alert-content">
              <div class="alert-title">{{ alert.title }}</div>
              <div class="alert-description">{{ alert.description }}</div>
              <div class="alert-meta">
                <span class="alert-vehicle">{{ alert.vehiclePlate }}</span>
                <span class="alert-time">{{ formatTime(alert.createdAt) }}</span>
              </div>
            </div>
            <div class="alert-actions">
              <el-button size="small" @click.stop="acknowledgeAlert(alert.id)">
                确认
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Refresh, FullScreen, Van, Warning, Odometer, Clock,
  InfoFilled, CircleCheck
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { Vehicle, Alert } from '@/api/types'
import dayjs from 'dayjs'

const router = useRouter()

// 监控统计数据
const monitoringStats = ref({
  activeVehicles: 0,
  activeRate: 0,
  alerts: 0,
  pendingAlerts: 0,
  runningTasks: 0,
  completionRate: 0,
  systemUptime: '0天 0小时'
})

// 实时车辆数据
const realTimeVehicles = ref<Vehicle[]>([])

// 实时告警数据
const realTimeAlerts = ref<Alert[]>([])

// 定时器
let refreshTimer: NodeJS.Timeout | null = null

// 获取车辆状态类型
const getVehicleStatusType = (status: number) => {
  const statusMap: Record<number, string> = {
    1: 'success',   // 正常
    2: 'warning',   // 维修中
    3: 'danger',    // 故障
    0: 'info'       // 停用
  }
  return statusMap[status] || 'info'
}

// 获取车辆状态文本
const getVehicleStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '正常运行',
    2: '维修中',
    3: '故障',
    0: '停用'
  }
  return statusMap[status] || '未知'
}

// 获取车辆状态样式类
const getVehicleStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    1: 'status-active',
    2: 'status-maintenance',
    3: 'status-fault',
    0: 'status-offline'
  }
  return classMap[status] || 'status-unknown'
}

// 获取告警严重程度样式类
const getAlertSeverityClass = (severity: string) => {
  const classMap: Record<string, string> = {
    high: 'alert-high',
    medium: 'alert-medium',
    low: 'alert-low'
  }
  return classMap[severity] || 'alert-unknown'
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm:ss')
}

// 查看车辆详情
const viewVehicleDetail = (vehicleId: string) => {
  router.push(`/vehicles/${vehicleId}`)
}

// 处理告警
const handleAlert = (alert: Alert) => {
  ElMessage.info(`处理告警: ${alert.title}`)
}

// 确认告警
const acknowledgeAlert = async (alertId: string) => {
  try {
    // TODO: 调用API确认告警
    ElMessage.success('告警已确认')
    // 刷新告警列表
    loadRealTimeAlerts()
  } catch (error) {
    ElMessage.error('确认告警失败')
  }
}

// 刷新数据
const refreshData = async () => {
  try {
    await Promise.all([
      loadMonitoringStats(),
      loadRealTimeVehicles(),
      loadRealTimeAlerts()
    ])
    ElMessage.success('数据刷新成功')
  } catch (error) {
    ElMessage.error('数据刷新失败')
  }
}

// 切换全屏监控
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 加载监控统计数据
const loadMonitoringStats = async () => {
  try {
    // TODO: 调用API获取统计数据
    // 模拟数据
    monitoringStats.value = {
      activeVehicles: 15,
      activeRate: 85,
      alerts: 3,
      pendingAlerts: 1,
      runningTasks: 8,
      completionRate: 92,
      systemUptime: '15天 8小时'
    }
  } catch (error) {
    console.error('Load monitoring stats failed:', error)
  }
}

// 加载实时车辆数据
const loadRealTimeVehicles = async () => {
  try {
    // TODO: 调用API获取实时车辆数据
    // 模拟数据
    realTimeVehicles.value = [
      {
        id: '1',
        plateNumber: '京A12345',
        vehicleType: '客运大巴',
        status: 1,
        location: 'T3航站楼',
        speed: 45,
        batteryLevel: 85,
        lastUpdate: new Date().toISOString(),
        currentTask: {
          taskName: 'T3-01接机任务',
          progress: 65
        }
      },
      {
        id: '2',
        plateNumber: '京B67890',
        vehicleType: '货运卡车',
        status: 3,
        location: '货机坪',
        speed: 0,
        batteryLevel: 30,
        lastUpdate: new Date().toISOString(),
        currentTask: null
      }
    ]
  } catch (error) {
    console.error('Load real-time vehicles failed:', error)
  }
}

// 加载实时告警数据
const loadRealTimeAlerts = async () => {
  try {
    // TODO: 调用API获取实时告警数据
    // 模拟数据
    realTimeAlerts.value = [
      {
        id: '1',
        title: '车辆故障告警',
        description: '京B67890车辆引擎温度过高',
        severity: 'high',
        vehiclePlate: '京B67890',
        createdAt: new Date().toISOString(),
        acknowledged: false
      },
      {
        id: '2',
        title: '任务超时提醒',
        description: 'T3-01接机任务执行超时',
        severity: 'medium',
        vehiclePlate: '京A12345',
        createdAt: new Date(Date.now() - 300000).toISOString(),
        acknowledged: false
      }
    ]
  } catch (error) {
    console.error('Load real-time alerts failed:', error)
  }
}

// 初始化数据
const initData = async () => {
  await refreshData()
  
  // 设置定时刷新
  refreshTimer = setInterval(() => {
    loadRealTimeVehicles()
    loadRealTimeAlerts()
  }, 30000) // 30秒刷新一次
}

// 组件挂载时初始化
onMounted(() => {
  initData()
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped lang="scss">
.monitoring-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;
    
    .page-title {
      font-size: 28px;
      font-weight: 700;
      color: var(--text-primary-color);
      margin-bottom: 8px;
    }
    
    .page-description {
      color: var(--text-regular-color);
      font-size: 16px;
      margin-bottom: 0;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
}

.monitoring-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  }
  
  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: white;
    
    &.active {
      background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    }
    
    &.warning {
      background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
    }
    
    &.success {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }
    
    &.info {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
  }
  
  .stat-content {
    flex: 1;
    
    .stat-value {
      font-size: 32px;
      font-weight: 700;
      color: var(--text-primary-color);
      line-height: 1;
      margin-bottom: 4px;
    }
    
    .stat-label {
      font-size: 14px;
      color: var(--text-regular-color);
      margin-bottom: 4px;
    }
    
    .stat-trend {
      font-size: 12px;
      color: var(--text-secondary-color);
    }
  }
}

.monitoring-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.monitoring-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  
  .section-header {
    padding: 20px;
    border-bottom: 1px solid var(--border-lighter-color);
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .section-title {
      font-size: 18px;
      font-weight: 600;
      color: var(--text-primary-color);
      margin: 0;
    }
  }
}

.vehicle-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  padding: 20px;
}

.vehicle-card {
  background: var(--background-extra-light-color);
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-left: 4px solid transparent;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }
  
  &.status-active {
    border-left-color: #67c23a;
  }
  
  &.status-maintenance {
    border-left-color: #e6a23c;
  }
  
  &.status-fault {
    border-left-color: #f56c6c;
  }
  
  &.status-offline {
    border-left-color: #909399;
  }
  
  .vehicle-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
    
    .vehicle-info {
      .plate-number {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-primary-color);
        margin-bottom: 4px;
      }
      
      .vehicle-type {
        font-size: 14px;
        color: var(--text-regular-color);
      }
    }
  }
  
  .vehicle-details {
    margin-bottom: 12px;
    
    .detail-item {
      display: flex;
      justify-content: space-between;
      margin-bottom: 6px;
      
      .label {
        font-size: 14px;
        color: var(--text-secondary-color);
      }
      
      .value {
        font-size: 14px;
        color: var(--text-primary-color);
        font-weight: 500;
      }
    }
  }
  
  .vehicle-progress {
    .progress-label {
      font-size: 12px;
      color: var(--text-secondary-color);
      margin-bottom: 8px;
    }
    
    .progress-text {
      font-size: 12px;
      color: var(--text-regular-color);
      margin-top: 4px;
      text-align: center;
    }
  }
}

.alerts-list {
  padding: 20px;
  max-height: 400px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: var(--background-extra-light-color);
  }
  
  &.alert-high {
    border-left: 4px solid #f56c6c;
    background: #fef0f0;
    
    &:hover {
      background: #fde2e2;
    }
  }
  
  &.alert-medium {
    border-left: 4px solid #e6a23c;
    background: #fdf6ec;
    
    &:hover {
      background: #fcefd8;
    }
  }
  
  &.alert-low {
    border-left: 4px solid #67c23a;
    background: #f0f9ff;
    
    &:hover {
      background: #e1f3ff;
    }
  }
  
  .alert-icon {
    font-size: 20px;
    color: var(--text-secondary-color);
    margin-top: 2px;
  }
  
  .alert-content {
    flex: 1;
    
    .alert-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary-color);
      margin-bottom: 4px;
    }
    
    .alert-description {
      font-size: 14px;
      color: var(--text-regular-color);
      margin-bottom: 8px;
      line-height: 1.4;
    }
    
    .alert-meta {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: var(--text-secondary-color);
      
      .alert-vehicle {
        font-weight: 500;
      }
    }
  }
  
  .alert-actions {
    display: flex;
    align-items: flex-start;
  }
}

@media (max-width: 1200px) {
  .monitoring-content {
    grid-template-columns: 1fr;
  }
  
  .vehicle-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 16px;
    
    .header-actions {
      width: 100%;
      justify-content: stretch;
      
      .el-button {
        flex: 1;
      }
    }
  }
  
  .monitoring-stats {
    grid-template-columns: 1fr;
  }
  
  .vehicle-grid {
    grid-template-columns: 1fr;
  }
}
</style>