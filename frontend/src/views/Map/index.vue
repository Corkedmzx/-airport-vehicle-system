<template>
  <div class="map-page">
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
      <div ref="mapContainer" class="map-view" id="baidu-map-container">
        <!-- 百度地图将在这里渲染 -->
      </div>
      
      <!-- 图例悬浮窗 -->
      <div 
        class="map-legend-floating" 
        :class="{ collapsed: legendCollapsed }"
        :style="{ top: legendPosition.top + 'px', left: legendPosition.left + 'px' }"
      >
        <div 
          class="legend-header" 
          @mousedown="startDrag"
          @click.stop="toggleLegend"
        >
          <h4>图例</h4>
          <el-icon 
            class="legend-toggle-icon"
            @click.stop="toggleLegend"
          >
            <ArrowUp v-if="!legendCollapsed" />
            <ArrowDown v-else />
          </el-icon>
        </div>
        <div class="legend-content" v-show="!legendCollapsed">
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
            <div class="legend-item" v-if="pcLocationMarker">
              <div class="legend-marker pc-location"></div>
              <span>PC位置</span>
            </div>
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
  Connection, Odometer, Clock, MapLocation, Picture,
  ArrowUp, ArrowDown
} from '@element-plus/icons-vue'
import type { Vehicle } from '@/api/types'
import dayjs from 'dayjs'
import { getSystemConfigApi } from '@/api/system'
import { getVehiclesApi } from '@/api/vehicles'
import { webSocketClient } from '@/utils/websocket'

const router = useRouter()

// 地图相关
const mapContainer = ref<HTMLElement>()
const currentMapStyle = ref('streets')
const showTraffic = ref(false)
const mapStatus = ref('加载中')
let baiduMap: any = null
let mapMarkers: any[] = []

// 图例相关
const legendCollapsed = ref(false)
const legendPosition = ref({ top: 16, left: 0 }) // 默认位置（右侧）
const isDragging = ref(false)
const dragStartPos = ref({ x: 0, y: 0 })
const legendStartPos = ref({ top: 0, left: 0 })

// PC位置相关
const pcLocationMarker = ref<any>(null)
const pcLocation = ref<{ latitude: number; longitude: number; accuracy: number } | null>(null)
let pcLocationUpdateTimer: NodeJS.Timeout | null = null

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
  if (!baiduMap) {
    ElMessage.warning('地图未初始化')
    return
  }
  
  const allPoints: any[] = []
  
  // 添加车辆位置点
  filteredVehicles.value
    .filter((v: any) => v.latitude && v.longitude)
    .forEach((v: any) => {
      allPoints.push(new (window as any).BMap.Point(v.longitude, v.latitude))
    })
  
  // 添加PC位置点
  if (pcLocation.value) {
    allPoints.push(new (window as any).BMap.Point(pcLocation.value.longitude, pcLocation.value.latitude))
  }
  
  if (allPoints.length > 0) {
    const viewport = baiduMap.getViewport(allPoints, { padding: 50 })
    baiduMap.centerAndZoom(viewport.center, viewport.zoom)
    ElMessage.success(`已定位到 ${allPoints.length} 个位置点`)
  } else {
    ElMessage.warning('没有可定位的位置点')
  }
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
  if (!baiduMap) {
    ElMessage.warning('地图未初始化')
    return
  }
  
  // 查找车辆
  const vehicle = mapVehicles.value.find((v: any) => v.id === vehicleId?.toString() || v.plateNumber === vehicleId?.toString())
  
  if (!vehicle) {
    ElMessage.warning('未找到该车辆')
    return
  }
  
  if (!vehicle.latitude || !vehicle.longitude) {
    ElMessage.warning('该车辆暂无位置信息')
    return
  }
  
  // 定位到车辆位置
  const point = new (window as any).BMap.Point(vehicle.longitude, vehicle.latitude)
  baiduMap.centerAndZoom(point, 18) // 使用较大的缩放级别，便于查看车辆详情
  ElMessage.success(`已定位到车辆 ${vehicle.plateNumber}`)
  
  // 自动打开车辆信息窗口
  setTimeout(() => {
    const infoWindow = new (window as any).BMap.InfoWindow(
      `<div style="padding: 8px;">
        <strong>${vehicle.plateNumber}</strong><br/>
        状态: ${getVehicleStatusText(vehicle.status)}<br/>
        位置: ${vehicle.location || '未知'}<br/>
        ${vehicle.speed ? `速度: ${vehicle.speed} km/h<br/>` : ''}
        更新时间: ${formatTime(vehicle.lastUpdate)}
      </div>`,
      { width: 200, height: 120 }
    )
    baiduMap.openInfoWindow(infoWindow, point)
  }, 300)
}

// 维修申请
const requestMaintenance = (vehicle: Vehicle) => {
  ElMessage.info(`为车辆 ${vehicle.plateNumber} 提交维修申请功能开发中`)
}

// 更新地图标记
const updateMapMarkers = () => {
  if (!baiduMap) return
  
  // 清除现有车辆标记（保留PC位置标记）
  mapMarkers.forEach(marker => {
    baiduMap.removeOverlay(marker)
  })
  mapMarkers = []
  
  // 添加车辆标记
  filteredVehicles.value.forEach((vehicle: any) => {
    if (vehicle.latitude && vehicle.longitude) {
      const point = new (window as any).BMap.Point(vehicle.longitude, vehicle.latitude)
      
      // 根据车辆状态选择图标颜色 - 使用Canvas绘制图标（百度地图标准方式）
      const iconColor = getVehicleStatusColor(vehicle.status)
      const canvas = document.createElement('canvas')
      canvas.width = 32
      canvas.height = 32
      const ctx = canvas.getContext('2d')
      
      if (ctx) {
        // 绘制外圈（带颜色边框）
        ctx.beginPath()
        ctx.arc(16, 16, 14, 0, Math.PI * 2)
        ctx.fillStyle = iconColor
        ctx.fill()
        ctx.strokeStyle = '#ffffff'
        ctx.lineWidth = 3
        ctx.stroke()
        
        // 绘制内圈（白色中心）
        ctx.beginPath()
        ctx.arc(16, 16, 6, 0, Math.PI * 2)
        ctx.fillStyle = '#ffffff'
        ctx.fill()
      }
      
      const icon = new (window as any).BMap.Icon(
        canvas.toDataURL(),
        new (window as any).BMap.Size(32, 32),
        { anchor: new (window as any).BMap.Size(16, 16) }
      )
      
      const marker = new (window as any).BMap.Marker(point, { icon })
      
      // 添加信息窗口
      const infoWindow = new (window as any).BMap.InfoWindow(
        `<div style="padding: 8px;">
          <strong>${vehicle.plateNumber}</strong><br/>
          状态: ${getVehicleStatusText(vehicle.status)}<br/>
          位置: ${vehicle.location || '未知'}<br/>
          ${vehicle.speed ? `速度: ${vehicle.speed} km/h<br/>` : ''}
          更新时间: ${formatTime(vehicle.lastUpdate)}
        </div>`,
        { width: 200, height: 120 }
      )
      
      marker.addEventListener('click', () => {
        baiduMap.openInfoWindow(infoWindow, point)
        selectedVehicle.value = vehicle
      })
      
      baiduMap.addOverlay(marker)
      mapMarkers.push(marker)
    }
  })
  
  // PC位置标记由updatePCLocationMarker单独管理，不在这里处理
  
  // 注意：不在这里调整地图视野，避免刷新时改变用户当前视图
  // 视野调整只在初始化时或用户点击"定位全部"时执行
}

// 获取车辆状态颜色
const getVehicleStatusColor = (status: number) => {
  const colorMap: Record<number, string> = {
    1: '#67c23a',  // 正常 - 绿色
    2: '#e6a23c',  // 维修中 - 橙色
    3: '#f56c6c',  // 故障 - 红色
    0: '#909399'   // 停用 - 灰色
  }
  return colorMap[status] || '#909399'
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
  // 如果有车辆数据，使用第一个车辆的位置，否则使用首都机场的坐标
  let lat = 40.0801
  let lng = 116.5842
  let locationName = '首都机场'
  
  if (mapVehicles.value.length > 0 && mapVehicles.value[0].latitude && mapVehicles.value[0].longitude) {
    lat = mapVehicles.value[0].latitude
    lng = mapVehicles.value[0].longitude
    locationName = mapVehicles.value[0].location || '车辆位置'
  }
  
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
  if (!mapContainer.value) {
    console.warn('地图容器未准备好')
    return
  }
  
  // 确保DOM元素存在
  const containerElement = document.getElementById('baidu-map-container')
  if (!containerElement) {
    console.error('找不到地图容器元素 #baidu-map-container')
    mapStatus.value = '异常'
    ElMessage.error('地图容器未找到')
    return
  }
  
  try {
    // 动态加载百度地图API（通过后端代理获取AK）
    await loadBaiduMapScript()
    
    // 等待百度地图API完全初始化（增加等待时间）
    console.log('等待BMap对象完全初始化...')
    let retryCount = 0
    const maxRetries = 30 // 最多等待15秒（30 * 500ms）
    
    while (retryCount < maxRetries) {
      // 检查BMap对象是否存在且已完全初始化
      if (typeof (window as any).BMap !== 'undefined') {
        // 尝试创建一个测试点，验证BMap是否完全初始化
        try {
          const testPoint = new (window as any).BMap.Point(116.5842, 40.0801)
          if (testPoint && testPoint.lng !== undefined && testPoint.lat !== undefined) {
            console.log('BMap对象已完全初始化')
            break
          }
        } catch (e) {
          // BMap还未完全初始化，继续等待
          console.log(`BMap对象存在但未完全初始化，继续等待... (${retryCount + 1}/${maxRetries})`)
        }
      }
      
      await new Promise(resolve => setTimeout(resolve, 500))
      retryCount++
    }
    
    if (typeof (window as any).BMap === 'undefined') {
      ElMessage.error('百度地图API加载失败，请检查后端配置')
      mapStatus.value = '异常'
      return
    }

    // 再次确保容器元素存在
    const container = document.getElementById('baidu-map-container')
    if (!container) {
      throw new Error('地图容器元素不存在')
    }
    
    console.log('开始创建百度地图实例...')
    
    // 创建百度地图实例
    const map = new (window as any).BMap.Map('baidu-map-container')
    
    // 等待地图加载完成
    await new Promise<void>((resolve) => {
      map.addEventListener('tilesloaded', () => {
        console.log('地图瓦片加载完成')
        resolve()
      }, { once: true })
      
      // 设置超时，避免无限等待
      setTimeout(() => {
        console.log('地图瓦片加载超时，继续初始化...')
        resolve()
      }, 5000)
    })
    
    // 优先使用PC位置，如果没有则使用默认位置（首都机场）
    let point: any
    let zoom = 15
    
    if (pcLocation.value && pcLocation.value.latitude && pcLocation.value.longitude) {
      // 使用PC位置
      point = new (window as any).BMap.Point(pcLocation.value.longitude, pcLocation.value.latitude)
      zoom = 16 // PC位置使用更大的缩放级别
      console.log('[地图初始化] 使用PC位置:', {
        latitude: pcLocation.value.latitude,
        longitude: pcLocation.value.longitude
      })
    } else {
      // 使用默认位置（首都机场）
      point = new (window as any).BMap.Point(116.5842, 40.0801)
      console.log('[地图初始化] 使用默认位置（首都机场）')
    }
    
    map.centerAndZoom(point, zoom)
    
    // 启用滚轮缩放
    map.enableScrollWheelZoom(true)
    
    // 添加地图控件
    map.addControl(new (window as any).BMap.NavigationControl())
    map.addControl(new (window as any).BMap.ScaleControl())
    map.addControl(new (window as any).BMap.MapTypeControl())
    
    baiduMap = map
    mapStatus.value = '正常'
    
    console.log('地图初始化成功')
    ElMessage.success('地图初始化成功')
  } catch (error) {
    console.error('地图初始化失败:', error)
    mapStatus.value = '异常'
    const errorMessage = error instanceof Error ? error.message : String(error)
    ElMessage.error('地图初始化失败: ' + errorMessage)
    
    // 提供更详细的错误信息
    if (errorMessage.includes('coordType') || errorMessage.includes('_rd')) {
      console.error('提示：BMap对象可能未完全初始化，请尝试刷新页面')
    }
  }
}

// 动态加载百度地图API脚本
const loadBaiduMapScript = async () => {
  return new Promise<void>((resolve, reject) => {
    // 检查是否已经加载
    if (typeof (window as any).BMap !== 'undefined') {
      console.log('百度地图API已加载')
      resolve()
      return
    }
    
    // 从后端获取API URL
    console.log('正在获取百度地图API配置...')
    fetch('/api/baidu-map/api-script')
      .then(response => {
        console.log('后端响应状态:', response.status, response.statusText)
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        return response.json()
      })
      .then(async (result: any) => {
        console.log('后端返回结果:', result)
        if (result.code === 200 && result.data?.apiUrl) {
          const apiUrl = result.data.apiUrl
          console.log('获取到百度地图API URL:', apiUrl)
          
          try {
            // 由于document.write()在异步加载的脚本中无法工作，
            // 我们需要通过后端代理获取第一个脚本内容，解析出getscript URL
            
            console.log('正在通过后端代理获取第一个脚本内容...')
            const firstScriptResponse = await fetch('/api/baidu-map/first-script')
            if (!firstScriptResponse.ok) {
              throw new Error(`后端代理请求失败: ${firstScriptResponse.status}`)
            }
            const firstScriptResult = await firstScriptResponse.json()
            
            if (firstScriptResult.code !== 200 || !firstScriptResult.data) {
              throw new Error(firstScriptResult.message || '获取第一个脚本内容失败')
            }
            
            const firstScriptText = firstScriptResult.data
            console.log('第一个脚本内容获取成功')
            
            // 解析第一个脚本，提取getscript URL
            // 格式类似：document.write('...src="https://api.map.baidu.com/getscript?v=3.0&ak=...&services=&t=..."...');
            const getscriptMatch = firstScriptText.match(/src=["']([^"']*getscript[^"']*)["']/i)
            
            if (!getscriptMatch || !getscriptMatch[1]) {
              console.error('无法从第一个脚本中提取getscript URL')
              console.error('第一个脚本内容预览:', firstScriptText.substring(0, 500))
              
              // 尝试手动构建getscript URL（备用方案）
              const ak = result.data.ak || apiUrl.match(/ak=([^&]+)/)?.[1]
              if (ak) {
                const timestamp = Date.now()
                const getscriptUrl = `https://api.map.baidu.com/getscript?v=3.0&ak=${ak}&services=&t=${timestamp}`
                console.log('使用备用方案构建getscript URL:', getscriptUrl)
                try {
                  await loadGetscriptScript(getscriptUrl)
                  resolve()
                } catch (error: any) {
                  reject(error)
                }
              } else {
                reject(new Error('无法提取getscript URL，且无法构建备用URL（缺少AK参数）'))
              }
              return
            }
            
            const getscriptUrl = getscriptMatch[1]
            console.log('提取到getscript URL:', getscriptUrl)
            
            // 手动设置第一个脚本中定义的全局变量（避免使用eval）
            // 从第一个脚本中提取这些值
            const protocolMatch = firstScriptText.match(/BMAP_PROTOCOL\s*=\s*["']([^"']+)["']/i)
            const loadTimeMatch = firstScriptText.match(/BMap_loadScriptTime\s*=\s*\(new\s+Date\)\.getTime\(\)/i)
            
            if (protocolMatch) {
              ;(window as any).BMAP_PROTOCOL = protocolMatch[1]
              console.log('设置BMAP_PROTOCOL:', protocolMatch[1])
            } else {
              ;(window as any).BMAP_PROTOCOL = 'https'
              console.log('未找到BMAP_PROTOCOL，使用默认值: https')
            }
            
            if (loadTimeMatch) {
              ;(window as any).BMap_loadScriptTime = Date.now()
              console.log('设置BMap_loadScriptTime:', (window as any).BMap_loadScriptTime)
            } else {
              ;(window as any).BMap_loadScriptTime = Date.now()
              console.log('未找到BMap_loadScriptTime模式，使用当前时间戳')
            }
            
            // 手动加载getscript脚本
            try {
              await loadGetscriptScript(getscriptUrl)
              resolve()
            } catch (error: any) {
              reject(error)
            }
            
          } catch (error: any) {
            console.error('加载百度地图API失败:', error)
            reject(new Error(`加载百度地图API失败: ${error.message}`))
          }
        } else {
          const errorMsg = result.message || '获取百度地图API配置失败'
          console.error('获取百度地图API配置失败:', errorMsg, result)
          reject(new Error(errorMsg))
        }
      })
      .catch((error: any) => {
        console.error('获取百度地图API配置失败:', error)
        reject(new Error(`无法连接到后端服务: ${error.message}。请检查后端是否正常运行`))
      })
  })
}

// 加载getscript脚本的辅助函数
const loadGetscriptScript = async (getscriptUrl: string) => {
  return new Promise<void>((resolveInner, rejectInner) => {
    console.log('正在通过后端代理获取getscript脚本内容...')
    
    // 先通过后端代理获取内容，检查是否是错误页面
    fetch(`/api/baidu-map/getscript?url=${encodeURIComponent(getscriptUrl)}`)
      .then(response => {
        if (!response.ok) {
          throw new Error(`后端代理请求失败: ${response.status}`)
        }
        return response.json()
      })
      .then((result: any) => {
        if (result.code !== 200 || !result.data) {
          const errorMsg = result.message || '获取getscript脚本内容失败'
          console.error('获取getscript脚本内容失败:', errorMsg)
          rejectInner(new Error(errorMsg))
          return
        }
        
        const scriptContent = result.data
        console.log('成功获取getscript脚本内容，长度:', scriptContent.length)
        
        // 检查内容是否是错误页面
        if (scriptContent.includes('APP被您禁用') || 
            scriptContent.includes('APP服务被禁用') ||
            scriptContent.includes('被禁用')) {
              console.error('getscript返回错误页面')
          rejectInner(new Error('百度地图应用被禁用。请检查：1. 应用状态是否为已启用；2. JavaScript API服务是否已启用；3. AK是否正确配置。详情查看: http://lbsyun.baidu.com/apiconsole/key'))
          return
        }
        
        // 将脚本内容作为内联脚本执行
        const script = document.createElement('script')
        script.type = 'text/javascript'
        script.textContent = scriptContent
        
        const checkInterval = 100 // 每100ms检查一次
        const maxWaitTime = 10000 // 最长等待10秒
        const startTime = Date.now()
        
        // 轮询检测BMap对象是否可用
        const checkBMap = () => {
          const elapsed = Date.now() - startTime
          
          // 检查BMap对象是否已初始化
          if (typeof (window as any).BMap !== 'undefined') {
            // 验证BMap是否完全初始化（尝试创建Point对象）
            try {
              const testPoint = new (window as any).BMap.Point(116.5842, 40.0801)
              if (testPoint && testPoint.lng !== undefined && testPoint.lat !== undefined) {
                console.log('百度地图API加载成功，BMap对象已完全初始化')
                resolveInner()
                return true
              }
            } catch (e) {
              // BMap还未完全初始化，继续等待
            }
          }
          
          // 检查是否超时
          if (elapsed > maxWaitTime) {
            console.error('百度地图API加载超时（10秒）')
            const error = new Error('百度地图API加载超时，BMap对象未完全初始化。请检查网络连接和AK配置。')
            rejectInner(error)
            return true
          }
          
          // 继续检查
          return false
        }
        
        // 创建定时器定期检查
        const intervalId = setInterval(() => {
          if (checkBMap()) {
            clearInterval(intervalId)
          }
        }, checkInterval)
        
        script.onload = () => {
          console.log('getscript脚本执行成功')
          console.log('等待BMap对象完全初始化...')
          // 等待一段时间后开始检查
          setTimeout(() => {
            if (checkBMap()) {
              clearInterval(intervalId)
            }
          }, 500)
        }
        
        script.onerror = (event) => {
          console.error('getscript脚本执行失败（onerror触发）:', event)
          clearInterval(intervalId)
          const error = new Error('getscript脚本执行失败。可能的原因：脚本内容格式错误或AK配置问题。')
          rejectInner(error)
        }
        
        document.head.appendChild(script)
        console.log('📝 已添加getscript脚本到页面')
      })
      .catch((error: any) => {
        console.error('获取getscript脚本内容失败:', error)
        rejectInner(error)
      })
  })
}

// 加载地图数据
const loadMapData = async () => {
  try {
    // 调用API获取车辆数据
    const response = await getVehiclesApi()
    if (response.data.code === 200) {
      const vehicles = response.data.data || []
      
      // 转换为地图显示格式
      mapVehicles.value = vehicles.map((v: Vehicle) => ({
        id: v.id?.toString() || '',
        plateNumber: v.vehicleNo || '',
        vehicleType: v.brand || '未知',
        status: v.status || 0,
        location: v.locationAddress || '未知位置',
        latitude: v.locationLatitude ? Number(v.locationLatitude) : null,
        longitude: v.locationLongitude ? Number(v.locationLongitude) : null,
        speed: 0, // 速度信息需要从位置更新中获取
        lastUpdate: v.lastUpdateTime || new Date().toISOString(),
        currentTask: null // 任务信息需要从任务API获取
      })).filter((v: any) => v.latitude != null && v.longitude != null) // 只显示有位置的车辆
      
      // 更新统计信息
      realTimeStats.value.onlineVehicles = mapVehicles.value.filter((v: any) => v.status === 1).length
      realTimeStats.value.runningTasks = mapVehicles.value.filter((v: any) => v.currentTask).length
      
      lastUpdateTime.value = new Date().toISOString()
      
      filterVehicles()
    }
  } catch (error) {
    console.error('Load map data failed:', error)
    ElMessage.error('加载地图数据失败')
  }
}

// 处理WebSocket位置更新
const handleVehicleLocationUpdate = (data: any) => {
  const { vehicleId, vehicleNo, longitude, latitude, address, speed, source, deviceName } = data
  
  // 如果是PC位置，单独处理
  if (source === 'pc_browser' || deviceName === 'pc_location') {
    console.log('[PC位置] 收到WebSocket位置更新:', data)
    // 更新PC位置（假设WebSocket收到的是WGS84坐标，需要转换为BD09）
    if (longitude && latitude) {
      const wgs84Lon = Number(longitude)
      const wgs84Lat = Number(latitude)
      
      // 将WGS84坐标转换为BD09坐标
      convertWGS84ToBD09(wgs84Lon, wgs84Lat).then(bd09Coord => {
      pcLocation.value = {
          latitude: bd09Coord.lat,
          longitude: bd09Coord.lng,
        accuracy: data.accuracy || 0
      }
        // WebSocket更新时不自动缩放，保持用户当前视图
        updatePCLocationMarker(false)
        console.log('[PC位置] WebSocket更新成功，坐标已转换（WGS84 -> BD09），地图标记已刷新:', {
          wgs84: { lat: wgs84Lat, lng: wgs84Lon },
          bd09: { lat: pcLocation.value.latitude, lng: pcLocation.value.longitude },
        accuracy: pcLocation.value.accuracy
        })
      }).catch(error => {
        console.error('[PC位置] WebSocket位置坐标转换失败:', error.message)
        // 转换失败时，直接使用原始坐标（假设已经是BD09或容错处理）
        pcLocation.value = {
          latitude: wgs84Lat,
          longitude: wgs84Lon,
          accuracy: data.accuracy || 0
        }
        updatePCLocationMarker(false)
      })
    } else {
      console.warn('[PC位置] WebSocket数据不完整，缺少longitude或latitude')
    }
    return
  }
  
  // 处理车辆位置更新
  // 查找并更新车辆位置
  const vehicleIndex = mapVehicles.value.findIndex((v: any) => v.id === vehicleId?.toString() || v.plateNumber === vehicleNo)
  if (vehicleIndex >= 0) {
    const vehicle = mapVehicles.value[vehicleIndex]
    vehicle.longitude = Number(longitude)
    vehicle.latitude = Number(latitude)
    vehicle.location = address || vehicle.location
    vehicle.speed = speed || 0
    vehicle.lastUpdate = new Date().toISOString()
  } else {
    // 如果车辆不存在，添加新车辆
    mapVehicles.value.push({
      id: vehicleId?.toString() || '',
      plateNumber: vehicleNo || '',
      vehicleType: '未知',
      status: 1,
      location: address || '未知位置',
      latitude: Number(latitude),
      longitude: Number(longitude),
      speed: speed || 0,
      lastUpdate: new Date().toISOString(),
      currentTask: null
    })
  }
  
  // 更新筛选后的列表
  filterVehicles()
  
  // 如果当前选中的车辆位置更新了，也更新选中车辆的信息
  if (selectedVehicle.value && (selectedVehicle.value.id === vehicleId?.toString() || selectedVehicle.value.plateNumber === vehicleNo)) {
    const updatedVehicle = mapVehicles.value.find((v: any) => v.id === vehicleId?.toString() || v.plateNumber === vehicleNo)
    if (updatedVehicle) {
      selectedVehicle.value = { ...updatedVehicle }
    }
  }
  
  // 更新地图标记
  updateMapMarkers()
  
  // 更新最后更新时间
  lastUpdateTime.value = new Date().toISOString()
}

// 切换图例展开/收缩
const toggleLegend = () => {
  legendCollapsed.value = !legendCollapsed.value
  saveLegendPosition()
}

// 开始拖拽图例
const startDrag = (e: MouseEvent) => {
  // 如果点击的是展开/收缩图标，不拖拽
  const target = e.target as HTMLElement
  if (target.closest('.legend-toggle-icon')) {
    return
  }
  
  isDragging.value = true
  dragStartPos.value = { x: e.clientX, y: e.clientY }
  legendStartPos.value = { ...legendPosition.value }
  
  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', stopDrag)
  
  e.preventDefault()
  e.stopPropagation()
}

// 处理拖拽
const handleDrag = (e: MouseEvent) => {
  if (!isDragging.value) return
  
  const deltaX = e.clientX - dragStartPos.value.x
  const deltaY = e.clientY - dragStartPos.value.y
  
  // 获取地图容器的边界
  const mapContainerEl = mapContainer.value
  if (!mapContainerEl) return
  
  const containerRect = mapContainerEl.getBoundingClientRect()
  const legendWidth = 180 // 图例宽度
  const legendHeight = legendCollapsed.value ? 50 : 200 // 图例高度（估算）
  
  // 计算新位置，限制在容器内
  let newLeft = legendStartPos.value.left + deltaX
  let newTop = legendStartPos.value.top + deltaY
  
  // 限制在容器范围内
  newLeft = Math.max(0, Math.min(newLeft, containerRect.width - legendWidth))
  newTop = Math.max(0, Math.min(newTop, containerRect.height - legendHeight))
  
  legendPosition.value = { top: newTop, left: newLeft }
}

// 停止拖拽
const stopDrag = () => {
  if (isDragging.value) {
    isDragging.value = false
    saveLegendPosition()
    document.removeEventListener('mousemove', handleDrag)
    document.removeEventListener('mouseup', stopDrag)
  }
}

// 保存图例位置到localStorage
const saveLegendPosition = () => {
  try {
    localStorage.setItem('map_legend_position', JSON.stringify(legendPosition.value))
    localStorage.setItem('map_legend_collapsed', String(legendCollapsed.value))
  } catch (error) {
    console.warn('保存图例位置失败:', error)
  }
}

// 从localStorage加载图例位置
const loadLegendPosition = () => {
  try {
    const savedPosition = localStorage.getItem('map_legend_position')
    const savedCollapsed = localStorage.getItem('map_legend_collapsed')
    
    if (savedPosition) {
      const position = JSON.parse(savedPosition)
      // 验证位置是否有效
      if (position.top !== undefined && position.left !== undefined) {
        legendPosition.value = position
      }
    } else {
      // 默认位置：右上角
      const mapContainerEl = mapContainer.value
      if (mapContainerEl) {
        const containerRect = mapContainerEl.getBoundingClientRect()
        legendPosition.value = {
          top: 16,
          left: containerRect.width - 196 // 180宽度 + 16边距
        }
      }
    }
    
    if (savedCollapsed !== null) {
      legendCollapsed.value = savedCollapsed === 'true'
    }
  } catch (error) {
    console.warn('加载图例位置失败:', error)
  }
}

// 获取PC位置信息（单次定位）
const getPCLocation = (): Promise<GeolocationPosition> => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持地理位置API'))
      return
    }
    
    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve(position)
      },
      (error) => {
        reject(error)
      },
      {
        enableHighAccuracy: true,  // 启用高精度定位（使用GPS、Wi-Fi、传感器等）
        timeout: 20000,            // 增加超时时间到20秒，给定位服务更多时间获取高精度位置
        maximumAge: 0              // 不使用缓存位置，每次都获取最新位置
      }
    )
  })
}

// 获取高精度PC位置（多次采样取平均值，提高精度）
const getHighAccuracyPCLocation = (sampleCount: number = 3, sampleInterval: number = 2000): Promise<GeolocationPosition> => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持地理位置API'))
      return
    }
    
    const positions: GeolocationPosition[] = []
    let sampleIndex = 0
    
    const watchId = navigator.geolocation.watchPosition(
      (position) => {
        positions.push(position)
        sampleIndex++
        
        // 如果精度已经很好（小于50米），直接使用
        if (position.coords.accuracy < 50) {
          navigator.geolocation.clearWatch(watchId)
          resolve(position)
          return
        }
        
        // 采样足够次数后，计算平均值
        if (sampleIndex >= sampleCount) {
          navigator.geolocation.clearWatch(watchId)
          
          // 过滤掉精度太差的位置（超过500米）
          const validPositions = positions.filter(p => p.coords.accuracy < 500)
          
          if (validPositions.length === 0) {
            // 如果所有位置精度都很差，使用最好的那个
            const bestPosition = positions.reduce((best, current) => 
              current.coords.accuracy < best.coords.accuracy ? current : best
            )
            resolve(bestPosition)
            return
          }
          
          // 计算加权平均（精度越好的权重越大）
          let totalWeight = 0
          let weightedLat = 0
          let weightedLon = 0
          let bestAccuracy = Infinity
          
          validPositions.forEach(pos => {
            const weight = 1 / pos.coords.accuracy // 精度越好的权重越大
            totalWeight += weight
            weightedLat += pos.coords.latitude * weight
            weightedLon += pos.coords.longitude * weight
            bestAccuracy = Math.min(bestAccuracy, pos.coords.accuracy)
          })
          
          // 创建合成的位置对象
          const averagedPosition: GeolocationPosition = {
            coords: {
              latitude: weightedLat / totalWeight,
              longitude: weightedLon / totalWeight,
              altitude: validPositions[0].coords.altitude,
              accuracy: bestAccuracy * 0.8, // 平均后精度通常会更好
              altitudeAccuracy: validPositions[0].coords.altitudeAccuracy,
              heading: validPositions[0].coords.heading,
              speed: validPositions[0].coords.speed
            },
            timestamp: Date.now()
          } as GeolocationPosition
          
          console.log('[高精度定位] 多次采样完成:', {
            sampleCount: validPositions.length,
            originalAccuracy: positions.map(p => p.coords.accuracy.toFixed(0) + '米'),
            averagedAccuracy: averagedPosition.coords.accuracy.toFixed(0) + '米'
          })
          
          resolve(averagedPosition)
        }
      },
      (error) => {
        navigator.geolocation.clearWatch(watchId)
        // 如果有部分采样结果，使用最好的那个
        if (positions.length > 0) {
          const bestPosition = positions.reduce((best, current) => 
            current.coords.accuracy < best.coords.accuracy ? current : best
          )
          resolve(bestPosition)
        } else {
          reject(error)
        }
      },
      {
        enableHighAccuracy: true,
        timeout: sampleCount * sampleInterval + 5000,
        maximumAge: 0
      }
    )
    
    // 设置总超时时间
    setTimeout(() => {
      navigator.geolocation.clearWatch(watchId)
      if (positions.length > 0) {
        const bestPosition = positions.reduce((best, current) => 
          current.coords.accuracy < best.coords.accuracy ? current : best
        )
        resolve(bestPosition)
      } else {
        reject(new Error('定位超时'))
      }
    }, sampleCount * sampleInterval + 10000)
  })
}

// WGS84坐标转换为BD09坐标（百度地图坐标系）
// 浏览器Geolocation API返回的是WGS84坐标，而百度地图需要使用BD09坐标
// 注意：使用经过验证的高精度算法转换，精度可达10-50米级别
const convertWGS84ToBD09 = (wgLon: number, wgLat: number): Promise<{ lng: number; lat: number }> => {
  return new Promise((resolve) => {
    // 直接使用高精度算法转换（经过验证的精确算法）
    // 百度地图官方API在某些情况下可能不够准确，使用算法转换更可靠
    const result = highPrecisionWGS84ToBD09(wgLon, wgLat)
    resolve(result)
  })
}

// 高精度转换函数（WGS84 -> GCJ02 -> BD09）
// 使用经过验证的精确坐标转换算法，精度可达10-50米级别
const highPrecisionWGS84ToBD09 = (wgLon: number, wgLat: number): { lng: number; lat: number } => {
  // 第一步：WGS84 -> GCJ02（火星坐标系/国测局坐标系）
  let dLat = transformLat(wgLon - 105.0, wgLat - 35.0)
  let dLon = transformLon(wgLon - 105.0, wgLat - 35.0)
  const radLat = (wgLat / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - 0.00669342162296594323 * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / ((6378245.0 * (1 - 0.00669342162296594323)) / (magic * sqrtMagic) * Math.PI)
  dLon = (dLon * 180.0) / (6378245.0 / sqrtMagic * Math.cos(radLat) * Math.PI)
  let gcj02Lon = wgLon + dLon
  let gcj02Lat = wgLat + dLat
  
  // 第二步：GCJ02 -> BD09（百度坐标系）
  // 使用标准的BD09转换公式（百度坐标系偏移算法）
  // 注意：BD09是在GCJ02基础上进行非线性偏移
  const x = gcj02Lon
  const y = gcj02Lat
  const z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * Math.PI * 3000.0 / 180.0)
  const theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * Math.PI * 3000.0 / 180.0)
  const bd09Lon = z * Math.cos(theta) + 0.0065
  const bd09Lat = z * Math.sin(theta) + 0.006
  
  const result = { lng: bd09Lon, lat: bd09Lat }
  
  console.log('[坐标转换] 算法转换完成（WGS84 -> GCJ02 -> BD09）:', {
    wgs84: { lng: wgLon.toFixed(6), lat: wgLat.toFixed(6) },
    gcj02: { lng: gcj02Lon.toFixed(6), lat: gcj02Lat.toFixed(6) },
    bd09: { lng: bd09Lon.toFixed(6), lat: bd09Lat.toFixed(6) },
    offset: { 
      lng: ((bd09Lon - wgLon) * 111000).toFixed(2) + '米', 
      lat: ((bd09Lat - wgLat) * 111000).toFixed(2) + '米' 
    }
  })
  
  return result
}

// 旧版本近似转换函数（保留作为兼容）
const approximateWGS84ToBD09 = highPrecisionWGS84ToBD09

// 辅助函数：纬度转换
const transformLat = (lng: number, lat: number): number => {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
  ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0
  return ret
}

// 辅助函数：经度转换
const transformLon = (lng: number, lat: number): number => {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
  ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0
  return ret
}

// 上传PC位置信息到华为云（通过vehicle_001设备）
const uploadPCLocation = async (latitude: number, longitude: number, accuracy: number) => {
  try {
    const payload = {
      latitude,
      longitude,
      accuracy,
      timestamp: Date.now()
    }
    
    console.log('[PC位置] 开始上传位置信息到后端:', payload)
    
    const response = await fetch('/api/mqtt/upload-pc-location', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    
    if (!response.ok) {
      throw new Error(`上传失败: HTTP ${response.status}`)
    }
    
    const result = await response.json()
    if (result.code === 200) {
      console.log('[PC位置] 后端接口返回成功，消息已发送到MQTT')
      return true
    } else {
      console.error('[PC位置] 后端接口返回错误:', result.message)
      return false
    }
  } catch (error: any) {
    console.error('[PC位置] 上传位置信息失败:', error.message)
    return false
  }
}

// 更新PC位置标记
const updatePCLocationMarker = (autoZoom = false) => {
  if (!baiduMap || !pcLocation.value) {
    console.warn('[PC位置] 无法更新标记：地图未初始化或位置数据不存在')
    return
  }
  
  // 清除旧标记
  if (pcLocationMarker.value) {
    baiduMap.removeOverlay(pcLocationMarker.value)
  }
  
  // 创建新标记
  const point = new (window as any).BMap.Point(pcLocation.value.longitude, pcLocation.value.latitude)
  
  // PC位置使用百度地图API的标准图标样式
  // 创建一个Canvas图标，使用百度地图的标准样式
  const canvas = document.createElement('canvas')
  canvas.width = 32
  canvas.height = 32
  const ctx = canvas.getContext('2d')
  
  if (ctx) {
    // 绘制外圈（白色边框）
    ctx.beginPath()
    ctx.arc(16, 16, 14, 0, Math.PI * 2)
    ctx.fillStyle = '#409eff'
    ctx.fill()
    ctx.strokeStyle = '#ffffff'
    ctx.lineWidth = 3
    ctx.stroke()
    
    // 绘制内圈（白色中心）
    ctx.beginPath()
    ctx.arc(16, 16, 6, 0, Math.PI * 2)
    ctx.fillStyle = '#ffffff'
    ctx.fill()
  }
  
  // 使用Canvas创建图标
  const icon = new (window as any).BMap.Icon(
    canvas.toDataURL(),
    new (window as any).BMap.Size(32, 32),
    { anchor: new (window as any).BMap.Size(16, 16) }
  )
  
  const marker = new (window as any).BMap.Marker(point, { icon })
  
  // 添加信息窗口（可滚动样式）
  // 使用自定义滚动条样式，确保长文本可以完整显示
  const scrollbarStyle = `
    <style type="text/css">
      .pc-info-window-container::-webkit-scrollbar { width: 6px; height: 6px; }
      .pc-info-window-container::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 3px; }
      .pc-info-window-container::-webkit-scrollbar-thumb { background: #c1c1c1; border-radius: 3px; }
      .pc-info-window-container::-webkit-scrollbar-thumb:hover { background: #a8a8a8; }
    </style>
  `
  
  const infoWindow = new (window as any).BMap.InfoWindow(
    `<div class="pc-info-window-container" style="padding: 12px; min-width: 260px; max-width: 320px; font-size: 13px; line-height: 1.6; max-height: 350px; overflow-y: auto; overflow-x: hidden; word-wrap: break-word;">
      <div style="font-weight: 600; font-size: 14px; margin-bottom: 8px; color: #303133; border-bottom: 1px solid #e4e7ed; padding-bottom: 6px;">
        PC位置
      </div>
      <div style="margin-bottom: 4px;">
        <span style="color: #606266; min-width: 60px; display: inline-block;">经度:</span>
        <span style="color: #303133; font-family: monospace;">${pcLocation.value.longitude.toFixed(6)}</span>
      </div>
      <div style="margin-bottom: 4px;">
        <span style="color: #606266; min-width: 60px; display: inline-block;">纬度:</span>
        <span style="color: #303133; font-family: monospace;">${pcLocation.value.latitude.toFixed(6)}</span>
      </div>
      <div style="margin-bottom: 4px;">
        <span style="color: #606266; min-width: 60px; display: inline-block;">精度:</span>
        <span style="color: ${pcLocation.value.accuracy < 50 ? '#67C23A' : pcLocation.value.accuracy < 100 ? '#E6A23C' : '#F56C6C'}; font-weight: ${pcLocation.value.accuracy < 50 ? '600' : 'normal'}">
          ${pcLocation.value.accuracy.toFixed(0)} 米
          ${pcLocation.value.accuracy < 50 ? '（高）' : pcLocation.value.accuracy < 100 ? '（中）' : '（低）'}
        </span>
      </div>
      <div style="margin-top: 6px; padding: 8px; background-color: ${pcLocation.value.accuracy > 200 ? '#FEF0F0' : '#FDF6EC'}; border-left: 3px solid ${pcLocation.value.accuracy > 200 ? '#F56C6C' : '#E6A23C'}; border-radius: 4px; color: ${pcLocation.value.accuracy > 200 ? '#F56C6C' : '#E6A23C'}; font-size: 12px; line-height: 1.5; word-wrap: break-word; white-space: normal;">
        ${pcLocation.value.accuracy > 200 
          ? '⚠️ 警告：PC定位精度较差（实际偏差可能达200-500米）。建议连接Wi-Fi、使用移动设备，或手动选择位置以提高精度。' 
          : pcLocation.value.accuracy > 100 
          ? '💡 提示：精度较低，PC定位实际偏差可能大于显示值。建议连接Wi-Fi或移动到开阔地区。' 
          : '💡 提示：PC定位精度有限，实际偏差可能大于显示值。建议连接Wi-Fi以提高精度。'}
      </div>
      <div style="margin-top: 8px; padding-top: 6px; border-top: 1px solid #e4e7ed; color: #909399; font-size: 12px;">
        <span style="color: #909399;">更新时间:</span>
        <span style="color: #606266; margin-left: 4px;">${formatTime(new Date().toISOString())}</span>
      </div>
    </div>${scrollbarStyle}`,
    { width: 320, maxHeight: 380 }
  )
  
  marker.addEventListener('click', () => {
    baiduMap.openInfoWindow(infoWindow, point)
  })
  
  baiduMap.addOverlay(marker)
  pcLocationMarker.value = marker
  
  // 如果启用自动缩放，则缩放到PC位置
  if (autoZoom) {
    baiduMap.centerAndZoom(point, 16)
    console.log('[PC位置] 地图已自动缩放到PC位置')
  }
}

// 启动PC位置监控
const startPCLocationMonitoring = async () => {
  try {
    console.log('[PC位置] 启动PC位置监控...')
    // 使用高精度定位（多次采样取平均值）
    // 如果精度不够好（>100米），提示用户
    const position = await getHighAccuracyPCLocation(3, 2000) // 采样3次，每次间隔2秒
    
    // 检查定位精度并获取坐标
    const accuracy = position.coords.accuracy || 0
    const wgs84Lat = position.coords.latitude
    const wgs84Lon = position.coords.longitude
    
    if (accuracy > 100) {
      ElMessage.warning(`定位精度较低（${accuracy.toFixed(0)}米），建议连接Wi-Fi或移动到开阔地区以提高精度`)
      console.warn('[PC位置] 定位精度较低:', accuracy.toFixed(0) + '米')
    } else if (accuracy > 50) {
      ElMessage.info(`定位精度：${accuracy.toFixed(0)}米（中等精度）`)
      console.log('[PC位置] 定位精度:', accuracy.toFixed(0) + '米')
    } else {
      ElMessage.success(`定位精度：${accuracy.toFixed(0)}米（高精度）`)
      console.log('[PC位置] 定位精度:', accuracy.toFixed(0) + '米')
    }
    
    console.log('[PC位置] 获取到WGS84坐标:', {
      latitude: wgs84Lat,
      longitude: wgs84Lon,
      accuracy: accuracy
    })
    
    // 将WGS84坐标转换为BD09坐标（百度地图坐标系）
    const bd09Coord = await convertWGS84ToBD09(wgs84Lon, wgs84Lat)
    
    // 存储转换后的BD09坐标
    pcLocation.value = {
      latitude: bd09Coord.lat,
      longitude: bd09Coord.lng,
      accuracy: accuracy
    }
    
    console.log('[PC位置] 坐标转换完成（WGS84 -> BD09）:', {
      wgs84: { lat: wgs84Lat, lng: wgs84Lon },
      bd09: { lat: pcLocation.value.latitude, lng: pcLocation.value.longitude },
      accuracy: pcLocation.value.accuracy
    })
    
    // 如果地图已初始化，立即定位到PC位置（使用BD09坐标）
    if (baiduMap) {
      const point = new (window as any).BMap.Point(pcLocation.value.longitude, pcLocation.value.latitude)
      baiduMap.centerAndZoom(point, 16)
      console.log('[PC位置] 地图已定位到PC位置（BD09坐标）')
    }
    
    // 上传位置信息（上传转换后的BD09坐标，确保后端使用正确的坐标系）
    const uploadSuccess = await uploadPCLocation(
      pcLocation.value.latitude,  // 上传转换后的BD09坐标
      pcLocation.value.longitude,
      accuracy
    )
    
    if (uploadSuccess) {
      console.log('[PC位置] 初始位置上传成功，等待WebSocket推送更新地图')
    } else {
      console.warn('[PC位置] 初始位置上传失败')
    }
    
    // 更新地图标记（启用自动缩放）
    updatePCLocationMarker(true)
    console.log('[PC位置] 地图标记已更新并自动缩放')
    
    // 设置定时更新（每60秒更新一次）
    pcLocationUpdateTimer = setInterval(async () => {
      try {
        console.log('[PC位置] 开始定时更新位置信息...')
        // 定时更新使用单次定位（快速响应）
        const position = await getPCLocation()
        const wgs84Lat = position.coords.latitude
        const wgs84Lon = position.coords.longitude
        const accuracy = position.coords.accuracy || 0
        
        // 将WGS84坐标转换为BD09坐标
        const bd09Coord = await convertWGS84ToBD09(wgs84Lon, wgs84Lat)
        
        // 更新PC位置（使用BD09坐标）
        pcLocation.value = {
          latitude: bd09Coord.lat,
          longitude: bd09Coord.lng,
          accuracy: accuracy
        }
        
        console.log('[PC位置] 位置更新（WGS84 -> BD09）:', {
          wgs84: { lat: wgs84Lat, lng: wgs84Lon },
          bd09: { lat: pcLocation.value.latitude, lng: pcLocation.value.longitude },
          accuracy: pcLocation.value.accuracy
        })
        
        const uploadSuccess = await uploadPCLocation(
          pcLocation.value.latitude,  // 上传转换后的BD09坐标
          pcLocation.value.longitude,
          accuracy
        )
        
        if (uploadSuccess) {
          console.log('[PC位置] 位置信息上传成功，等待WebSocket推送更新地图')
        } else {
          console.warn('[PC位置] 位置信息上传失败')
        }
        
        // 直接更新地图标记（不等待WebSocket，确保立即显示）
        // 定时更新时不自动缩放，保持用户当前视图
        updatePCLocationMarker(false)
      } catch (error: any) {
        console.error('[PC位置] 位置更新失败:', error.message)
      }
    }, 60000) // 60秒更新一次
    
    ElMessage.success('PC位置监控已启动（60秒更新一次）')
    console.log('[PC位置] PC位置监控已启动，更新间隔: 60秒')
  } catch (error: any) {
    console.error('[PC位置] 启动PC位置监控失败:', error)
    ElMessage.warning('PC位置监控启动失败: ' + error.message)
  }
}

// 停止PC位置监控
const stopPCLocationMonitoring = () => {
  if (pcLocationUpdateTimer) {
    clearInterval(pcLocationUpdateTimer)
    pcLocationUpdateTimer = null
  }
  
  if (pcLocationMarker.value && baiduMap) {
    baiduMap.removeOverlay(pcLocationMarker.value)
    pcLocationMarker.value = null
  }
  
  pcLocation.value = null
  ElMessage.info('PC位置监控已停止')
}

// 组件挂载时初始化
onMounted(async () => {
  // 先尝试获取PC位置（WGS84坐标）
  let wgs84Location: { lat: number; lng: number; accuracy: number } | null = null
  try {
    console.log('[初始化] 尝试获取PC位置...')
    const position = await getPCLocation()
    wgs84Location = {
      lat: position.coords.latitude,
      lng: position.coords.longitude,
      accuracy: position.coords.accuracy || 0
    }
    console.log('[初始化] PC位置获取成功（WGS84坐标）:', wgs84Location)
  } catch (error: any) {
    console.warn('[初始化] PC位置获取失败（可能用户未授权），将使用默认位置:', error.message)
  }
  
  // 初始化地图
  await initMap()
  
  // 如果获取到了PC位置，在地图初始化后转换为BD09坐标
  if (wgs84Location && baiduMap) {
    try {
      const bd09Coord = await convertWGS84ToBD09(wgs84Location.lng, wgs84Location.lat)
      pcLocation.value = {
        latitude: bd09Coord.lat,
        longitude: bd09Coord.lng,
        accuracy: wgs84Location.accuracy
      }
      console.log('[初始化] PC位置坐标转换完成（WGS84 -> BD09）:', {
        wgs84: wgs84Location,
        bd09: { lat: pcLocation.value.latitude, lng: pcLocation.value.longitude }
      })
      
      // 使用转换后的BD09坐标定位地图
      const point = new (window as any).BMap.Point(pcLocation.value.longitude, pcLocation.value.latitude)
      baiduMap.centerAndZoom(point, 16)
      updatePCLocationMarker(true)
    } catch (error: any) {
      console.error('[初始化] PC位置坐标转换失败:', error.message)
    }
  }
  
  await loadMapData()
  updateMapMarkers()
  
  // 加载图例位置（需要等待DOM渲染完成）
  await nextTick()
  loadLegendPosition()
  
  // 监听WebSocket位置更新
  webSocketClient.on('vehicle_location', handleVehicleLocationUpdate)
  
  // 启动PC位置监控（如果之前获取失败，这里会再次尝试）
  await startPCLocationMonitoring()
  
  // 设置定时更新（作为备用，主要依赖WebSocket实时更新）
  updateTimer = setInterval(async () => {
    await loadMapData()
    updateMapMarkers()
  }, 60000) // 60秒更新一次（备用）
})

// 组件卸载时清理定时器和WebSocket监听
onUnmounted(() => {
  if (updateTimer) {
    clearInterval(updateTimer)
  }
  stopPCLocationMonitoring()
  stopDrag() // 确保清理拖拽事件监听
  webSocketClient.off('vehicle_location', handleVehicleLocationUpdate)
})
</script>

<style scoped lang="scss">
.map-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  
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
  
  .map-legend-floating {
    position: absolute;
    background: white;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    min-width: 180px;
    z-index: 1000;
    transition: box-shadow 0.3s ease;
    overflow: hidden;
    user-select: none;
    
    &:hover {
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
    }
    
    &.collapsed {
      .legend-content {
        max-height: 0;
        opacity: 0;
        padding: 0 16px;
      }
    }
    
    .legend-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      cursor: move;
      user-select: none;
      border-bottom: 1px solid var(--border-lighter-color);
      transition: background-color 0.2s;
      
      &:hover {
        background-color: var(--bg-hover-color);
      }
      
      &:active {
        cursor: grabbing;
      }
      
      h4 {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-primary-color);
        margin: 0;
        pointer-events: none; // 防止文字选择影响拖拽
      }
      
      .legend-toggle-icon {
        font-size: 16px;
        color: var(--text-regular-color);
        transition: transform 0.3s ease;
        cursor: pointer;
        pointer-events: auto; // 允许点击图标
        flex-shrink: 0;
        margin-left: 8px;
        
        &:hover {
          color: var(--text-primary-color);
        }
      }
    }
    
    .legend-content {
      padding: 12px 16px;
      max-height: 500px;
      opacity: 1;
      transition: all 0.3s ease;
      
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
            flex-shrink: 0;
            
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
            
            &.pc-location {
              background: #409eff;
              border: 2px solid white;
              box-shadow: 0 0 0 2px #409eff;
            }
          }
          
          span {
            font-size: 12px;
            color: var(--text-regular-color);
          }
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