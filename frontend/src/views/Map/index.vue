<template>
  <div class="map-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">地图监控</h1>
      <p class="page-description">实时地图监控车辆位置、轨迹和状态</p>
      <div class="header-actions">
        <el-button type="primary" @click="locateAll">
          <el-icon><Location /></el-icon>
          定位全部
        </el-button>
        <el-button @click="toggleTraffic">
          <el-icon><Connection /></el-icon>
          {{ showTraffic ? '关闭路况' : '显示路况' }}
        </el-button>
        <el-button @click="exportMap">
          <el-icon><Download /></el-icon>
          导出地图
        </el-button>
      </div>
    </div>

    <!-- 地图工具栏 -->
    <div class="map-toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索车辆或位置"
            style="width: 300px"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="filter-group">
          <el-select
            v-model="statusFilter"
            placeholder="车辆状态"
            style="width: 120px"
            clearable
            @change="filterVehicles"
          >
            <el-option label="全部" value="" />
            <el-option label="正常" value="1" />
            <el-option label="维修中" value="2" />
            <el-option label="故障" value="3" />
            <el-option label="停用" value="0" />
          </el-select>
          
          <el-select
            v-model="taskFilter"
            placeholder="任务状态"
            style="width: 120px"
            clearable
            @change="filterVehicles"
          >
            <el-option label="全部" value="" />
            <el-option label="待分配" value="pending" />
            <el-option label="执行中" value="running" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </div>
      </div>
      
      <div class="toolbar-right">
        <el-button-group>
          <el-button @click="setMapStyle('streets')" :type="currentMapStyle === 'streets' ? 'primary' : ''">
            街道
          </el-button>
          <el-button @click="setMapStyle('satellite')" :type="currentMapStyle === 'satellite' ? 'primary' : ''">
            卫星
          </el-button>
          <el-button @click="setMapStyle('hybrid')" :type="currentMapStyle === 'hybrid' ? 'primary' : ''">
            混合
          </el-button>
        </el-button-group>
        
        <el-button @click="refreshMap">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 地图容器 -->
    <div class="map-container">
      <div ref="mapContainer" class="map-view">
        <div class="map-placeholder">
          <div class="baidu-map-link">
            <h3>地图监控</h3>
            <p>点击下方按钮打开地图查看车辆位置</p>
            <el-button type="primary" @click="openMap">
              <el-icon><MapLocation /></el-icon>
              打开地图
            </el-button>
            <el-button @click="showMapScreenshot">
              <el-icon><Picture /></el-icon>
              查看地图截图
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 图例 -->
      <div class="map-legend">
        <h4>图例</h4>
        <div class="legend-items">
          <div class="legend-item">
            <div class="legend-marker active"></div>
            <span>正常运行</span>
          </div>
          <div class="legend-item">
            <div class="legend-marker maintenance"></div>
            <span>维修中</span>
          </div>
          <div class="legend-item">
            <div class="legend-marker fault"></div>
            <span>故障</span>
          </div>
          <div class="legend-item">
            <div class="legend-marker offline"></div>
            <span>离线</span>
          </div>
          <div class="legend-item">
            <div class="legend-marker task-running"></div>
            <span>执行任务</span>
          </div>
        </div>
      </div>
      
      <!-- 车辆信息面板 -->
      <div v-if="selectedVehicle" class="vehicle-panel">
        <div class="panel-header">
          <h4>车辆信息</h4>
          <el-button 
            type="text" 
            size="small" 
            @click="selectedVehicle = null"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="panel-content">
          <div class="vehicle-basic">
            <div class="basic-item">
              <label>车牌号:</label>
              <span>{{ selectedVehicle.plateNumber }}</span>
            </div>
            <div class="basic-item">
              <label>车辆类型:</label>
              <span>{{ selectedVehicle.vehicleType }}</span>
            </div>
            <div class="basic-item">
              <label>状态:</label>
              <el-tag :type="getVehicleStatusType(selectedVehicle.status)" size="small">
                {{ getVehicleStatusText(selectedVehicle.status) }}
              </el-tag>
            </div>
            <div class="basic-item">
              <label>当前位置:</label>
              <span>{{ selectedVehicle.location || '未知' }}</span>
            </div>
            <div class="basic-item">
              <label>速度:</label>
              <span>{{ selectedVehicle.speed || 0 }} km/h</span>
            </div>
            <div class="basic-item">
              <label>最后更新:</label>
              <span>{{ formatTime(selectedVehicle.lastUpdate) }}</span>
            </div>
          </div>
          
          <div v-if="selectedVehicle.currentTask" class="current-task">
            <h5>当前任务</h5>
            <div class="task-info">
              <div class="task-item">
                <label>任务名称:</label>
                <span>{{ selectedVehicle.currentTask.taskName }}</span>
              </div>
              <div class="task-item">
                <label>进度:</label>
                <el-progress 
                  :percentage="selectedVehicle.currentTask.progress" 
                  :show-text="false"
                  :stroke-width="6"
                />
                <span>{{ selectedVehicle.currentTask.progress }}%</span>
              </div>
            </div>
          </div>
          
          <div class="panel-actions">
            <el-button size="small" @click="viewVehicleDetail(selectedVehicle.id)">
              查看详情
            </el-button>
            <el-button size="small" @click="trackVehicle(selectedVehicle.id)">
              跟踪车辆
            </el-button>
            <el-button 
              v-if="selectedVehicle.status === 3"
              type="warning" 
              size="small" 
              @click="requestMaintenance(selectedVehicle)"
            >
              维修申请
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 实时信息栏 -->
    <div class="realtime-info">
      <div class="info-item">
        <el-icon><Connection /></el-icon>
        <span>在线车辆: {{ realTimeStats.onlineVehicles }}</span>
      </div>
      <div class="info-item">
        <el-icon><Odometer /></el-icon>
        <span>执行任务: {{ realTimeStats.runningTasks }}</span>
      </div>
      <div class="info-item">
        <el-icon><Clock /></el-icon>
        <span>最后更新: {{ formatTime(lastUpdateTime) }}</span>
      </div>
      <div class="info-item">
        <el-icon><MapLocation /></el-icon>
        <span>地图状态: {{ mapStatus }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Location, Download, Search, Refresh, Close,
  Connection, Odometer, Clock, MapLocation, Picture
} from '@element-plus/icons-vue'
import type { Vehicle } from '@/api/types'
import dayjs from 'dayjs'
import { getSystemConfigApi } from '@/api/system'

const router = useRouter()

// 地图相关
const mapContainer = ref<HTMLElement>()
const currentMapStyle = ref('streets')
const showTraffic = ref(false)
const mapStatus = ref('加载中')

// 搜索和筛选
const searchKeyword = ref('')
const statusFilter = ref('')
const taskFilter = ref('')

// 车辆数据
const mapVehicles = ref<Vehicle[]>([])
const filteredVehicles = ref<Vehicle[]>([])
const selectedVehicle = ref<Vehicle | null>(null)

// 实时统计
const realTimeStats = ref({
  onlineVehicles: 0,
  runningTasks: 0
})

// 最后更新时间
const lastUpdateTime = ref(new Date().toISOString())

// 定时器
let updateTimer: NodeJS.Timeout | null = null

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

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('HH:mm:ss')
}

// 搜索处理
const handleSearch = () => {
  filterVehicles()
  updateMapMarkers()
}

// 筛选车辆
const filterVehicles = () => {
  filteredVehicles.value = mapVehicles.value.filter(vehicle => {
    // 状态筛选
    if (statusFilter.value && vehicle.status.toString() !== statusFilter.value) {
      return false
    }
    
    // 任务筛选
    if (taskFilter.value) {
      if (taskFilter.value === 'pending' && vehicle.currentTask) return false
      if (taskFilter.value === 'running' && !vehicle.currentTask) return false
      if (taskFilter.value === 'completed') return false // 已完成任务的车辆通常不在地图上显示
    }
    
    // 关键词搜索
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      return vehicle.plateNumber.toLowerCase().includes(keyword) ||
             (vehicle.location && vehicle.location.toLowerCase().includes(keyword))
    }
    
    return true
  })
  
  updateMapMarkers()
}

// 设置地图样式
const setMapStyle = (style: string) => {
  currentMapStyle.value = style
  ElMessage.info(`地图样式已切换为: ${style}`)
  // TODO: 实际切换地图样式
}

// 定位全部车辆
const locateAll = () => {
  ElMessage.info('正在定位所有车辆...')
  // TODO: 实际定位功能
}

// 切换路况显示
const toggleTraffic = () => {
  showTraffic.value = !showTraffic.value
  ElMessage.info(showTraffic.value ? '路况已开启' : '路况已关闭')
  // TODO: 实际切换路况
}

// 导出地图
const exportMap = () => {
  ElMessage.info('导出地图功能开发中')
}

// 刷新地图
const refreshMap = async () => {
  ElMessage.info('正在刷新地图数据...')
  await loadMapData()
  updateMapMarkers()
  ElMessage.success('地图数据已刷新')
}

// 查看车辆详情
const viewVehicleDetail = (vehicleId: string) => {
  router.push(`/vehicles/${vehicleId}`)
}

// 跟踪车辆
const trackVehicle = (vehicleId: string) => {
  ElMessage.info(`开始跟踪车辆 ${vehicleId}`)
  // TODO: 实际跟踪功能
}

// 维修申请
const requestMaintenance = (vehicle: Vehicle) => {
  ElMessage.info(`为车辆 ${vehicle.plateNumber} 提交维修申请功能开发中`)
}

// 更新地图标记
const updateMapMarkers = () => {
  // TODO: 实际更新地图标记
  console.log('Updating map markers:', filteredVehicles.value.length)
}

// 地图供应商配置
const mapProvider = ref('baidu')

// 加载地图供应商配置
const loadMapProvider = async () => {
  try {
    const response = await getSystemConfigApi('map.provider')
    if (response.data.code === 200) {
      mapProvider.value = response.data.data || 'baidu'
    }
  } catch (error: any) {
    console.error('加载地图供应商配置失败:', error)
    mapProvider.value = 'baidu' // 默认使用百度地图
  }
}

// 打开地图（根据配置动态切换）
const openMap = () => {
  // 构建地图URL，使用首都机场的坐标
  // 注意：百度地图使用BD-09坐标系，高德和腾讯使用GCJ-02坐标系
  // 这里使用GCJ-02坐标（高德/腾讯标准），百度地图会自动转换
  const lat = 40.0801
  const lng = 116.5842
  const locationName = '首都机场'
  let url = ''
  let providerName = ''
  
  switch (mapProvider.value) {
    case 'baidu':
      // 百度地图：使用marker API格式，经纬度顺序为 lat,lng
      url = `https://api.map.baidu.com/marker?location=${lat},${lng}&title=${encodeURIComponent(locationName)}&content=${encodeURIComponent(locationName)}&output=html&src=airport-vehicle-system`
      providerName = '百度地图'
      break
    case 'gaode':
      // 高德地图：使用URI Scheme，经纬度顺序为 lng,lat
      url = `https://uri.amap.com/marker?position=${lng},${lat}&name=${encodeURIComponent(locationName)}`
      providerName = '高德地图'
      break
    case 'tencent':
      // 腾讯地图：使用URI API，经纬度顺序为 lat,lng
      url = `https://apis.map.qq.com/uri/v1/marker?marker=coord:${lat},${lng};title:${encodeURIComponent(locationName)}&referer=airport-vehicle-system`
      providerName = '腾讯地图'
      break
    default:
      url = `https://api.map.baidu.com/marker?location=${lat},${lng}&title=${encodeURIComponent(locationName)}&content=${encodeURIComponent(locationName)}&output=html&src=airport-vehicle-system`
      providerName = '百度地图'
  }
  
  window.open(url, '_blank')
  ElMessage.success(`已在新窗口打开${providerName}`)
}

// 显示地图截图
const showMapScreenshot = () => {
  ElMessage.info('地图截图功能：可以在此处显示机场地图的静态截图')
  // 这里可以显示一个对话框展示地图截图
}

// 初始化地图
const initMap = async () => {
  await nextTick()
  if (!mapContainer.value) return
  
  try {
    // 加载地图供应商配置
    await loadMapProvider()
    // 地图使用外部链接，不需要初始化
    mapStatus.value = '正常'
  } catch (error) {
    console.error('Map initialization failed:', error)
    mapStatus.value = '异常'
    ElMessage.error('地图初始化失败')
  }
}

// 加载地图数据
const loadMapData = async () => {
  try {
    // TODO: 调用API获取地图车辆数据
    // 模拟数据
    mapVehicles.value = [
      {
        id: '1',
        plateNumber: '京A12345',
        vehicleType: '客运大巴',
        status: 1,
        location: 'T3航站楼',
        latitude: 40.0801,
        longitude: 116.5842,
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
        location: '货机坪A',
        latitude: 40.0789,
        longitude: 116.5828,
        speed: 0,
        batteryLevel: 30,
        lastUpdate: new Date().toISOString(),
        currentTask: null
      },
      {
        id: '3',
        plateNumber: '京C11111',
        vehicleType: '特种车辆',
        status: 1,
        location: '跑道边',
        latitude: 40.0823,
        longitude: 116.5867,
        speed: 25,
        batteryLevel: 92,
        lastUpdate: new Date().toISOString(),
        currentTask: {
          taskName: '跑道维护任务',
          progress: 30
        }
      }
    ]
    
    // 更新统计信息
    realTimeStats.value.onlineVehicles = mapVehicles.value.filter(v => v.status === 1).length
    realTimeStats.value.runningTasks = mapVehicles.value.filter(v => v.currentTask).length
    
    lastUpdateTime.value = new Date().toISOString()
    
    filterVehicles()
  } catch (error) {
    console.error('Load map data failed:', error)
    ElMessage.error('加载地图数据失败')
  }
}

// 组件挂载时初始化
onMounted(async () => {
  await initMap()
  await loadMapData()
  updateMapMarkers()
  
  // 设置定时更新
  updateTimer = setInterval(async () => {
    await loadMapData()
    updateMapMarkers()
  }, 30000) // 30秒更新一次
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (updateTimer) {
    clearInterval(updateTimer)
  }
})
</script>

<style scoped lang="scss">
.map-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;
    padding: 16px 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    
    .page-title {
      font-size: 24px;
      font-weight: 700;
      color: var(--text-primary-color);
      margin-bottom: 8px;
    }
    
    .page-description {
      color: var(--text-regular-color);
      font-size: 14px;
      margin-bottom: 0;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
}

.map-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  
  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .search-box {
      flex-shrink: 0;
    }
    
    .filter-group {
      display: flex;
      gap: 12px;
    }
  }
  
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.map-container {
  flex: 1;
  position: relative;
  background: #f5f5f5;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  
  .map-view {
    width: 100%;
    height: 100%;
    background: #e8e8e8;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-secondary-color);
    font-size: 16px;
    position: relative;
    
    // 百度地图链接按钮
    .baidu-map-link {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      padding: 20px 40px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
      text-align: center;
      z-index: 10;
      
      h3 {
        margin: 0 0 12px 0;
        color: var(--text-primary-color);
        font-size: 18px;
      }
      
      p {
        margin: 0 0 20px 0;
        color: var(--text-regular-color);
        font-size: 14px;
      }
      
      .el-button {
        margin: 0 8px;
      }
    }
    
    // 地图截图占位
    .map-placeholder {
      width: 100%;
      height: 100%;
      background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600"><rect fill="%23f0f0f0" width="800" height="600"/><text x="400" y="300" text-anchor="middle" fill="%23999" font-size="24" font-family="Arial">地图加载中...</text></svg>');
      background-size: cover;
      background-position: center;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-secondary-color);
      font-size: 18px;
    }
  }
  
  .map-legend {
    position: absolute;
    top: 16px;
    right: 16px;
    background: white;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    min-width: 160px;
    
    h4 {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary-color);
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid var(--border-lighter-color);
    }
    
    .legend-items {
      .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .legend-marker {
          width: 12px;
          height: 12px;
          border-radius: 50%;
          
          &.active {
            background: #67c23a;
          }
          
          &.maintenance {
            background: #e6a23c;
          }
          
          &.fault {
            background: #f56c6c;
          }
          
          &.offline {
            background: #909399;
          }
          
          &.task-running {
            background: #409eff;
            animation: pulse 2s infinite;
          }
        }
        
        span {
          font-size: 12px;
          color: var(--text-regular-color);
        }
      }
    }
  }
  
  .vehicle-panel {
    position: absolute;
    bottom: 16px;
    left: 16px;
    background: white;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    min-width: 320px;
    max-width: 400px;
    
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 8px;
      border-bottom: 1px solid var(--border-lighter-color);
      
      h4 {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-primary-color);
        margin: 0;
      }
    }
    
    .panel-content {
      .vehicle-basic {
        margin-bottom: 16px;
        
        .basic-item {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8px;
          
          label {
            font-size: 14px;
            color: var(--text-secondary-color);
          }
          
          span {
            font-size: 14px;
            color: var(--text-primary-color);
            font-weight: 500;
          }
        }
      }
      
      .current-task {
        margin-bottom: 16px;
        
        h5 {
          font-size: 14px;
          font-weight: 600;
          color: var(--text-primary-color);
          margin-bottom: 8px;
        }
        
        .task-info {
          .task-item {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 8px;
            
            label {
              font-size: 12px;
              color: var(--text-secondary-color);
              min-width: 60px;
            }
            
            span {
              font-size: 12px;
              color: var(--text-primary-color);
            }
          }
        }
      }
      
      .panel-actions {
        display: flex;
        gap: 8px;
      }
    }
  }
}

.realtime-info {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 12px 20px;
  background: white;
  border-radius: 12px;
  margin-top: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  
  .info-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--text-regular-color);
    
    .el-icon {
      color: var(--primary-color);
    }
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 1200px) {
  .map-toolbar {
    flex-direction: column;
    gap: 16px;
    
    .toolbar-left,
    .toolbar-right {
      width: 100%;
      justify-content: center;
    }
    
    .toolbar-left {
      flex-wrap: wrap;
      gap: 12px;
    }
  }
  
  .vehicle-panel {
    left: 50%;
    transform: translateX(-50%);
    bottom: 80px;
    min-width: 280px;
    max-width: 320px;
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
  
  .map-toolbar {
    .toolbar-left {
      .search-box {
        width: 100% !important;
      }
      
      .filter-group {
        width: 100%;
        justify-content: space-between;
        
        .el-select {
          flex: 1;
        }
      }
    }
  }
  
  .realtime-info {
    flex-wrap: wrap;
    gap: 12px;
    
    .info-item {
      flex: 1;
      min-width: 120px;
      justify-content: center;
    }
  }
}
</style>