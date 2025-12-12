<template>
  <div class="dispatch-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">调度中心</h1>
      <p class="page-description">智能调度系统，优化车辆分配和任务执行</p>
      <div class="header-actions">
        <el-button type="primary" @click="autoDispatch">
          <el-icon><Operation /></el-icon>
          智能调度
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 调度概览 -->
    <div class="dispatch-overview">
      <div class="overview-card">
        <div class="card-icon pending">
          <el-icon><List /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ dispatchStats.pendingTasks }}</div>
          <div class="card-label">待分配任务</div>
          <div class="card-trend">需要调度</div>
        </div>
      </div>

      <div class="overview-card">
        <div class="card-icon available">
          <el-icon><Van /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ dispatchStats.availableVehicles }}</div>
          <div class="card-label">可用车辆</div>
          <div class="card-trend">空闲 {{ dispatchStats.idleRate }}%</div>
        </div>
      </div>

      <div class="overview-card">
        <div class="card-icon efficiency">
          <el-icon><Odometer /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ dispatchStats.dispatchEfficiency }}%</div>
          <div class="card-label">调度效率</div>
          <div class="card-trend">较昨日 {{ dispatchStats.efficiencyChange > 0 ? '+' : '' }}{{ dispatchStats.efficiencyChange }}%</div>
        </div>
      </div>

      <div class="overview-card">
        <div class="card-icon optimized">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ dispatchStats.avgResponseTime }}分钟</div>
          <div class="card-label">平均响应时间</div>
          <div class="card-trend">响应迅速</div>
        </div>
      </div>
    </div>

    <!-- 调度内容区域 -->
    <div class="dispatch-content">
      <!-- 待分配任务 -->
      <div class="dispatch-section">
        <div class="section-header">
          <h3 class="section-title">待分配任务 ({{ pendingTasks.length }})</h3>
          <div class="section-actions">
            <el-button type="primary" size="small" @click="autoAssignAll">
              自动分配全部
            </el-button>
          </div>
        </div>
        
        <div class="tasks-table">
          <el-table
            v-loading="loading"
            :data="pendingTasks"
            style="width: 100%"
            @selection-change="handleTaskSelection"
          >
            <el-table-column type="selection" width="55" />
            
            <el-table-column prop="taskName" label="任务名称" min-width="150">
              <template #default="{ row }">
                <div class="task-info">
                  <div class="task-name">{{ row.taskName }}</div>
                  <div class="task-id">ID: {{ row.id }}</div>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="priority" label="优先级" width="100">
              <template #default="{ row }">
                <el-tag 
                  :type="getPriorityType(row.priority)" 
                  size="small"
                >
                  {{ getPriorityText(row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="startLocation" label="起点" min-width="120" />
            <el-table-column prop="endLocation" label="终点" min-width="120" />
            
            <el-table-column prop="startTime" label="开始时间" width="150">
              <template #default="{ row }">
                {{ formatDateTime(row.startTime) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="estimatedDuration" label="预计时长" width="100">
              <template #default="{ row }">
                {{ row.estimatedDuration }}分钟
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="manualAssign(row)"
                >
                  手动分配
                </el-button>
                <el-button 
                  type="warning" 
                  size="small" 
                  @click="editTask(row)"
                >
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 车辆状态 -->
      <div class="dispatch-section">
        <div class="section-header">
          <h3 class="section-title">车辆状态 ({{ availableVehicles.length }})</h3>
          <div class="section-actions">
            <el-select 
              v-model="vehicleFilter" 
              placeholder="筛选状态" 
              size="small" 
              style="width: 120px"
              @change="filterVehicles"
            >
              <el-option label="全部" value="" />
              <el-option label="空闲" value="idle" />
              <el-option label="执行中" value="running" />
              <el-option label="维修中" value="maintenance" />
            </el-select>
          </div>
        </div>
        
        <div class="vehicles-grid">
          <div
            v-for="vehicle in filteredVehicles"
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
              <div class="detail-row">
                <span class="label">位置:</span>
                <span class="value">{{ vehicle.location || '未知' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">距离:</span>
                <span class="value">{{ vehicle.distanceToTasks || 0 }}km</span>
              </div>
              <div class="detail-row">
                <span class="label">负载:</span>
                <span class="value">{{ vehicle.currentLoad || 0 }}%</span>
              </div>
              <div class="detail-row">
                <span class="label">效率评分:</span>
                <span class="value efficiency-score">{{ vehicle.efficiencyScore || 0 }}</span>
              </div>
            </div>
            
            <div class="vehicle-actions">
              <el-button 
                v-if="vehicle.status === 1" 
                type="primary" 
                size="small" 
                @click.stop="assignTaskToVehicle(vehicle)"
              >
                分配任务
              </el-button>
              <el-button 
                v-else-if="vehicle.status === 3" 
                type="warning" 
                size="small" 
                @click.stop="requestMaintenance(vehicle)"
              >
                维修申请
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 手动分配对话框 -->
    <el-dialog
      v-model="assignDialogVisible"
      title="手动分配任务"
      width="500px"
      @close="resetAssignForm"
    >
      <el-form 
        ref="assignFormRef"
        :model="assignForm"
        :rules="assignRules"
        label-width="100px"
      >
        <el-form-item label="任务">
          <el-input v-model="assignForm.taskName" disabled />
        </el-form-item>
        
        <el-form-item label="分配车辆" prop="vehicleId">
          <el-select 
            v-model="assignForm.vehicleId" 
            placeholder="请选择车辆"
            style="width: 100%"
          >
            <el-option
              v-for="vehicle in availableVehiclesForAssign"
              :key="vehicle.id"
              :label="`${vehicle.plateNumber} (${vehicle.location})`"
              :value="vehicle.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="预计时长" prop="estimatedDuration">
          <el-input-number 
            v-model="assignForm.estimatedDuration" 
            :min="1" 
            :max="480"
            style="width: 100%"
          />
          <span style="margin-left: 8px; color: var(--text-secondary-color);">分钟</span>
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input 
            v-model="assignForm.notes" 
            type="textarea" 
            :rows="3"
            placeholder="请输入分配备注"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="assignDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAssign">
            确认分配
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import {
  Operation, Refresh, List, Van, Odometer, TrendCharts
} from '@element-plus/icons-vue'
import type { DispatchTask, Vehicle } from '@/api/types'
import dayjs from 'dayjs'

const router = useRouter()

// 调度统计数据
const dispatchStats = ref({
  pendingTasks: 0,
  availableVehicles: 0,
  idleRate: 0,
  dispatchEfficiency: 0,
  efficiencyChange: 0,
  avgResponseTime: 0
})

// 待分配任务列表
const pendingTasks = ref<DispatchTask[]>([])

// 车辆列表
const availableVehicles = ref<Vehicle[]>([])

// 筛选后的车辆列表
const filteredVehicles = ref<Vehicle[]>([])

// 车辆筛选条件
const vehicleFilter = ref('')

// 加载状态
const loading = ref(false)

// 分配对话框
const assignDialogVisible = ref(false)
const assignFormRef = ref<FormInstance>()

// 分配表单
const assignForm = ref({
  taskId: '',
  taskName: '',
  vehicleId: '',
  estimatedDuration: 0,
  notes: ''
})

// 分配表单验证规则
const assignRules = {
  vehicleId: [
    { required: true, message: '请选择车辆', trigger: 'change' }
  ],
  estimatedDuration: [
    { required: true, message: '请输入预计时长', trigger: 'blur' }
  ]
}

// 可用于分配的车辆
const availableVehiclesForAssign = computed(() => {
  return availableVehicles.value.filter(vehicle => vehicle.status === 1)
})

// 获取优先级类型
const getPriorityType = (priority: number) => {
  const typeMap: Record<number, string> = {
    1: 'danger',    // 高
    2: 'warning',   // 中
    3: 'info'       // 低
  }
  return typeMap[priority] || 'info'
}

// 获取优先级文本
const getPriorityText = (priority: number) => {
  const textMap: Record<number, string> = {
    1: '高',
    2: '中',
    3: '低'
  }
  return textMap[priority] || '未知'
}

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
    1: '空闲',
    2: '维修中',
    3: '故障',
    0: '停用'
  }
  return statusMap[status] || '未知'
}

// 获取车辆状态样式类
const getVehicleStatusClass = (status: number) => {
  const classMap: Record<number, string> = {
    1: 'status-idle',
    2: 'status-maintenance',
    3: 'status-fault',
    0: 'status-offline'
  }
  return classMap[status] || 'status-unknown'
}

// 格式化日期时间
const formatDateTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm')
}

// 任务选择处理
const handleTaskSelection = (selection: DispatchTask[]) => {
  console.log('Selected tasks:', selection)
}

// 车辆筛选
const filterVehicles = () => {
  if (!vehicleFilter.value) {
    filteredVehicles.value = availableVehicles.value
    return
  }
  
  filteredVehicles.value = availableVehicles.value.filter(vehicle => {
    switch (vehicleFilter.value) {
      case 'idle':
        return vehicle.status === 1
      case 'running':
        return vehicle.status === 3 && vehicle.currentTask
      case 'maintenance':
        return vehicle.status === 2
      default:
        return true
    }
  })
}

// 查看车辆详情
const viewVehicleDetail = (vehicleId: string) => {
  router.push(`/vehicles/${vehicleId}`)
}

// 编辑任务
const editTask = (task: DispatchTask) => {
  router.push(`/tasks/${task.id}`)
}

// 智能调度
const autoDispatch = async () => {
  try {
    await ElMessageBox.confirm(
      '将根据任务优先级、车辆位置和效率进行智能调度，是否继续？',
      '智能调度',
      {
        confirmButtonText: '确认调度',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    // 智能调度逻辑：根据任务优先级和车辆位置自动分配
    const unassignedTasks = pendingTasks.value
    const availableVehicles = availableVehicles.value
    
    if (unassignedTasks.length === 0) {
      ElMessage.warning('没有待分配的任务')
      return
    }
    
    if (availableVehicles.length === 0) {
      ElMessage.warning('没有可用的车辆')
      return
    }
    
    // 按优先级排序任务
    const sortedTasks = [...unassignedTasks].sort((a, b) => {
      // 优先级：1-高, 2-中, 3-低
      return a.priority - b.priority
    })
    
    let assignedCount = 0
    for (const task of sortedTasks) {
      if (availableVehicles.length === 0) break
      
      // 选择第一个可用车辆
      const vehicle = availableVehicles[0]
      try {
        const { assignTaskApi } = await import('@/api/tasks')
        await assignTaskApi(Number(task.id), Number(vehicle.id))
        assignedCount++
        // 从可用车辆列表中移除已分配的车辆
        const index = availableVehicles.findIndex(v => v.id === vehicle.id)
        if (index > -1) {
          availableVehicles.splice(index, 1)
        }
      } catch (error) {
        console.error(`分配任务 ${task.taskNo} 失败:`, error)
      }
    }
    
    ElMessage.success(`智能调度完成，已分配 ${assignedCount} 个任务`)
    await loadData()
  } catch {
    // 用户取消
  }
}

// 自动分配全部
const autoAssignAll = async () => {
  try {
    await ElMessageBox.confirm(
      `将自动分配所有 ${pendingTasks.value.length} 个待分配任务，是否继续？`,
      '自动分配',
      {
        confirmButtonText: '确认分配',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const unassignedTasks = pendingTasks.value
    const availableVehicles = availableVehicles.value
    
    if (unassignedTasks.length === 0) {
      ElMessage.warning('没有待分配的任务')
      return
    }
    
    if (availableVehicles.length === 0) {
      ElMessage.warning('没有可用的车辆')
      return
    }
    
    let assignedCount = 0
    for (let i = 0; i < Math.min(unassignedTasks.length, availableVehicles.length); i++) {
      const task = unassignedTasks[i]
      const vehicle = availableVehicles[i % availableVehicles.length]
      
      try {
        const { assignTaskApi } = await import('@/api/tasks')
        await assignTaskApi(Number(task.id), Number(vehicle.id))
        assignedCount++
      } catch (error) {
        console.error(`分配任务 ${task.taskNo} 失败:`, error)
      }
    }
    
    ElMessage.success(`自动分配完成，已分配 ${assignedCount} 个任务`)
    await loadData()
  } catch {
    // 用户取消
  }
}

// 手动分配
const manualAssign = (task: DispatchTask) => {
  assignForm.value = {
    taskId: task.id,
    taskName: task.taskName,
    vehicleId: '',
    estimatedDuration: task.estimatedDuration || 60,
    notes: ''
  }
  assignDialogVisible.value = true
}

// 分配任务给车辆
const assignTaskToVehicle = async (vehicle: Vehicle) => {
  if (pendingTasks.value.length === 0) {
    ElMessage.warning('没有待分配的任务')
    return
  }
  
  // 选择第一个待分配任务进行分配
  const task = pendingTasks.value[0]
  manualAssign(task)
  assignForm.value.vehicleId = vehicle.id.toString()
}

// 维修申请
const requestMaintenance = (vehicle: Vehicle) => {
  ElMessage.info(`为车辆 ${vehicle.plateNumber} 提交维修申请功能开发中`)
}

// 重置分配表单
const resetAssignForm = () => {
  assignForm.value = {
    taskId: '',
    taskName: '',
    vehicleId: '',
    estimatedDuration: 0,
    notes: ''
  }
}

// 确认分配
const confirmAssign = async () => {
  if (!assignFormRef.value) return
  
  try {
    await assignFormRef.value.validate()
    
    // 调用分配API
    const { assignTaskApi } = await import('@/api/tasks')
    const response = await assignTaskApi(
      Number(assignForm.value.taskId),
      Number(assignForm.value.vehicleId)
    )
    
    if (response.data.code === 200) {
      ElMessage.success('任务分配成功')
      assignDialogVisible.value = false
      await loadData()
    } else {
      ElMessage.error(response.data.message || '分配失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '分配失败')
    }
  }
}

// 刷新数据
const refreshData = async () => {
  await loadData()
  ElMessage.success('数据已刷新')
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadDispatchStats(),
      loadPendingTasks(),
      loadAvailableVehicles()
    ])
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载调度统计数据
const loadDispatchStats = async () => {
  try {
    // TODO: 调用API获取统计数据
    // 模拟数据
    dispatchStats.value = {
      pendingTasks: 8,
      availableVehicles: 12,
      idleRate: 75,
      dispatchEfficiency: 92,
      efficiencyChange: 5,
      avgResponseTime: 3.2
    }
  } catch (error) {
    console.error('Load dispatch stats failed:', error)
  }
}

// 加载待分配任务
const loadPendingTasks = async () => {
  try {
    const { getPendingTasksApi } = await import('@/api/tasks')
    const response = await getPendingTasksApi()
    if (response.data.code === 200) {
      pendingTasks.value = Array.isArray(response.data.data) ? response.data.data : []
    }
  } catch (error) {
    console.error('Load pending tasks failed:', error)
    pendingTasks.value = []
  }
}

// 加载可用车辆（旧代码保留作为备用）
const loadAvailableVehiclesOld = async () => {
  try {
    // 模拟数据
    availableVehicles.value = [
      {
        id: '1',
        taskName: 'T3-01接机任务',
        priority: 1,
        startLocation: 'T3航站楼',
        endLocation: '货机坪A',
        startTime: new Date(Date.now() + 600000).toISOString(),
        estimatedDuration: 45,
        status: 1
      },
      {
        id: '2',
        taskName: '货运配送任务',
        priority: 2,
        startLocation: '货机坪A',
        endLocation: '货运中心',
        startTime: new Date(Date.now() + 900000).toISOString(),
        estimatedDuration: 30,
        status: 1
      }
    ]
  } catch (error) {
    console.error('Load pending tasks failed:', error)
  }
}

// 加载可用车辆
const loadAvailableVehicles = async () => {
  try {
    const { getActiveVehiclesApi } = await import('@/api/vehicles')
    const response = await getActiveVehiclesApi()
    if (response.data.code === 200) {
      const vehicles = Array.isArray(response.data.data) ? response.data.data : []
      availableVehicles.value = vehicles.map(v => ({
        id: v.id.toString(),
        plateNumber: v.vehicleNo,
        vehicleType: `类型${v.vehicleTypeId}`,
        status: v.status,
        location: v.locationAddress || '未知',
        distanceToTasks: 0,
        currentLoad: 0,
        efficiencyScore: 90
      }))
      filteredVehicles.value = availableVehicles.value
    }
  } catch (error) {
    console.error('Load available vehicles failed:', error)
    availableVehicles.value = []
    filteredVehicles.value = []
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.dispatch-page {
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

.dispatch-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.overview-card {
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
  
  .card-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: white;
    
    &.pending {
      background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
    }
    
    &.available {
      background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    }
    
    &.efficiency {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    }
    
    &.optimized {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
  }
  
  .card-content {
    flex: 1;
    
    .card-value {
      font-size: 32px;
      font-weight: 700;
      color: var(--text-primary-color);
      line-height: 1;
      margin-bottom: 4px;
    }
    
    .card-label {
      font-size: 14px;
      color: var(--text-regular-color);
      margin-bottom: 4px;
    }
    
    .card-trend {
      font-size: 12px;
      color: var(--text-secondary-color);
    }
  }
}

.dispatch-content {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.dispatch-section {
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
    
    .section-actions {
      display: flex;
      gap: 12px;
    }
  }
}

.tasks-table {
  padding: 20px;
}

.task-info {
  .task-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary-color);
    margin-bottom: 4px;
  }
  
  .task-id {
    font-size: 12px;
    color: var(--text-secondary-color);
  }
}

.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
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
  
  &.status-idle {
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
    
    .detail-row {
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
        
        &.efficiency-score {
          color: #67c23a;
          font-weight: 600;
        }
      }
    }
  }
  
  .vehicle-actions {
    display: flex;
    gap: 8px;
  }
}

@media (max-width: 1200px) {
  .dispatch-overview {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .vehicles-grid {
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
  
  .dispatch-overview {
    grid-template-columns: 1fr;
  }
  
  .vehicles-grid {
    grid-template-columns: 1fr;
  }
}
</style>