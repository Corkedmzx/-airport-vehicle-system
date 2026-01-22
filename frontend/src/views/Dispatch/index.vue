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
                <el-button 
                  v-if="row.status === 4"
                  type="success" 
                  size="small" 
                  @click="resendTask(row)"
                >
                  重新发送
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 进行中任务 -->
      <div class="dispatch-section">
        <div class="section-header">
          <h3 class="section-title">进行中任务 ({{ inProgressTasks.length }})</h3>
          <div class="section-actions">
            <el-button @click="refreshData" size="small">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
        
        <div class="tasks-table">
          <el-table
            v-loading="loading"
            :data="inProgressTasks"
            style="width: 100%"
          >
            <el-table-column prop="taskName" label="任务名称" min-width="150">
              <template #default="{ row }">
                <div class="task-info">
                  <div class="task-name">{{ row.taskName }}</div>
                  <div class="task-id">编号: {{ row.taskNo }}</div>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="taskType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.taskType }}</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 3 ? 'primary' : 'warning'" size="small">
                  {{ row.status === 3 ? '执行中' : '已分配' }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="assignedVehicleId" label="分配车辆" width="120">
              <template #default="{ row }">
                {{ getVehicleNo(row.assignedVehicleId) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="startLocation" label="起点" min-width="120" />
            <el-table-column prop="endLocation" label="终点" min-width="120" />
            
            <el-table-column prop="progress" label="进度" width="120">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.progress || 0"
                  :status="row.status === 4 ? 'success' : undefined"
                  :show-text="false"
                  :stroke-width="6"
                />
                <span style="font-size: 12px; color: #666;">{{ row.progress || 0 }}%</span>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button 
                  v-if="canCompleteTask(row)"
                  type="success" 
                  size="small" 
                  @click="completeTask(row)"
                >
                  完成任务
                </el-button>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="viewTaskDetail(row)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 车辆状态 -->
      <div class="dispatch-section">
        <div class="section-header">
          <h3 class="section-title">车辆状态 ({{ filteredVehicles.length }})</h3>
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
              <el-option label="已分配" value="assigned" />
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
                <el-tag :type="getVehicleStatusType(vehicle.status, vehicle)" size="small">
                  {{ getVehicleStatusText(vehicle.status, vehicle) }}
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
            
            <div class="vehicle-task-info" v-if="vehicle.currentTask">
              <div class="task-name">当前任务: {{ vehicle.currentTask.taskName }}</div>
              <div class="task-status">
                <el-tag 
                  :type="vehicle.currentTask.status === 3 ? 'primary' : 'warning'" 
                  size="small"
                >
                  {{ vehicle.currentTask.status === 3 ? '执行中' : '已分配' }}
                </el-tag>
              </div>
            </div>
            
            <div class="vehicle-actions">
              <el-button 
                v-if="vehicle.status === 1 && !vehicle.hasTask" 
                type="primary" 
                size="small" 
                @click.stop="assignTaskToVehicle(vehicle)"
              >
                分配任务
              </el-button>
              <el-button 
                v-if="vehicle.hasTask && !vehicle.hasRunningTask && vehicle.currentTask"
                type="danger" 
                size="small" 
                @click.stop="unassignTask(vehicle.currentTask)"
              >
                取消分配
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
        <el-form-item label="任务" prop="taskId">
          <el-select 
            v-model="assignForm.taskId" 
            placeholder="请选择任务"
            style="width: 100%"
            @change="handleTaskChange"
          >
            <el-option
              v-for="task in pendingTasks"
              :key="task.id"
              :label="`${task.taskName} (${task.taskNo})`"
              :value="task.id"
            />
          </el-select>
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
        
        <!-- 根据任务类型显示不同的分配选项 -->
        <el-form-item 
          v-if="selectedTaskType === '维护调度'"
          label="分配维修员" 
          prop="maintenanceUsername"
        >
          <el-select 
            v-model="assignForm.maintenanceUsername" 
            placeholder="请选择维修员"
            style="width: 100%"
            filterable
            @focus="loadMaintenance"
          >
            <el-option
              v-for="maintenance in availableMaintenance"
              :key="maintenance.id"
              :label="`${maintenance.realName || maintenance.username} (${maintenance.username})`"
              :value="maintenance.username"
            >
              <span>{{ maintenance.realName || maintenance.username }}</span>
              <span style="color: #8492a6; font-size: 13px; margin-left: 8px;">{{ maintenance.username }}</span>
              <span v-if="maintenance.email" style="color: #8492a6; font-size: 12px; margin-left: 8px;">{{ maintenance.email }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item 
          v-else
          label="分配司机" 
          prop="driverUsername"
        >
          <el-select 
            v-model="assignForm.driverUsername" 
            placeholder="请选择司机"
            style="width: 100%"
            filterable
            @focus="loadDrivers"
          >
            <el-option
              v-for="driver in availableDrivers"
              :key="driver.id"
              :label="`${driver.realName || driver.username} (${driver.username})`"
              :value="driver.username"
            >
              <span>{{ driver.realName || driver.username }}</span>
              <span style="color: #8492a6; font-size: 13px; margin-left: 8px;">{{ driver.username }}</span>
              <span v-if="driver.email" style="color: #8492a6; font-size: 12px; margin-left: 8px;">{{ driver.email }}</span>
            </el-option>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import {
  Operation, Refresh, List, Van, Odometer, TrendCharts
} from '@element-plus/icons-vue'
import type { DispatchTask, Vehicle } from '@/api/types'
import { isDriver, isMaintenance, hasPermission } from '@/utils/permission'
import { useUserStore } from '@/store/user'
import dayjs from 'dayjs'

const userStore = useUserStore()

// 扩展Vehicle类型，添加任务相关属性和显示属性
interface VehicleWithTask extends Vehicle {
  plateNumber?: string
  vehicleType?: string
  location?: string
  distanceToTasks?: number
  currentLoad?: number
  efficiencyScore?: number
  hasTask?: boolean
  hasRunningTask?: boolean
  currentTask?: DispatchTask  // 当前任务（已分配或执行中）
}

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

// 进行中任务列表（状态为2-已分配或3-执行中）
const inProgressTasks = ref<DispatchTask[]>([])

// 车辆列表
const availableVehicles = ref<VehicleWithTask[]>([])

// 自动刷新定时器
let refreshTimer: NodeJS.Timeout | null = null

// 筛选后的车辆列表
const filteredVehicles = ref<VehicleWithTask[]>([])

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
  driverUsername: '',
  maintenanceUsername: '',
  estimatedDuration: 0,
  notes: ''
})

// 可用司机列表
const availableDrivers = ref<any[]>([])
const driversLoaded = ref(false)

// 可用维修员列表
const availableMaintenance = ref<any[]>([])
const maintenanceLoaded = ref(false)

// 当前选中的任务类型（用于判断显示司机还是维修员）
const selectedTaskType = computed(() => {
  if (!assignForm.value.taskId) return ''
  const task = pendingTasks.value.find(t => t.id === Number(assignForm.value.taskId))
  return task?.taskType || ''
})

// 分配表单验证规则（动态验证，根据任务类型）
const assignRules = computed(() => {
  const isMaintenanceTask = selectedTaskType.value === '维护调度'
  
  return {
    taskId: [
      { required: true, message: '请选择任务', trigger: 'change' }
    ],
    vehicleId: [
      { required: true, message: '请选择车辆', trigger: 'change' }
    ],
    driverUsername: isMaintenanceTask ? [] : [
      { required: true, message: '请选择司机', trigger: 'change' }
    ],
    maintenanceUsername: !isMaintenanceTask ? [] : [
      { required: true, message: '请选择维修员', trigger: 'change' }
    ],
    estimatedDuration: [
      { required: true, message: '请输入预计时长', trigger: 'blur' }
    ]
  }
})

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
const getVehicleStatusType = (status: number, vehicle?: VehicleWithTask) => {
  // 优先检查车辆状态是否为维修中(2)或故障(3)，这些状态应该直接显示
  if (status === 2) {
    return 'warning'  // 维修中 - 黄色
  }
  if (status === 3) {
    return 'danger'   // 故障 - 红色
  }
  if (status === 0) {
    return 'info'    // 停用 - 灰色
  }
  
  // 对于正常状态(1)的车辆，根据任务状态来判断
  if (vehicle) {
    if (vehicle.hasRunningTask) {
      return 'primary'  // 执行中 - 蓝色
    }
    if (vehicle.hasTask) {
      return 'warning'  // 已分配 - 黄色
    }
  }
  
  // 正常状态且没有任务，显示空闲
  return 'success'   // 空闲 - 绿色
}

// 获取车辆状态文本
const getVehicleStatusText = (status: number, vehicle?: VehicleWithTask) => {
  // 优先检查车辆状态是否为维修中(2)或故障(3)，这些状态应该直接显示
  if (status === 2) {
    return '维修中'
  }
  if (status === 3) {
    return '故障'
  }
  if (status === 0) {
    return '停用'
  }
  
  // 对于正常状态(1)的车辆，根据任务状态来判断
  if (vehicle) {
    if (vehicle.hasRunningTask) {
      return '执行中'
    }
    if (vehicle.hasTask) {
      return '已分配'
    }
  }
  
  // 正常状态且没有任务，显示空闲
  return '空闲'
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
    // 显示全部车辆
    filteredVehicles.value = availableVehicles.value
    return
  }
  
  filteredVehicles.value = availableVehicles.value.filter(vehicle => {
    switch (vehicleFilter.value) {
      case 'idle':
        // 空闲：状态为1且没有任务
        return vehicle.status === 1 && !vehicle.hasTask
      case 'assigned':
        // 已分配：有已分配的任务（任务状态为2）且没有正在执行的任务
        // 注意：现在车辆状态不再改变，完全通过hasTask来判断
        return vehicle.hasTask && !vehicle.hasRunningTask
      case 'running':
        // 执行中：有正在执行的任务（任务状态为3）
        return vehicle.hasRunningTask === true
      case 'maintenance':
        // 维修中：状态为2（数据库定义中2是维修中）
        return vehicle.status === 2
      default:
        return true
    }
  })
  
  console.log('筛选车辆:', {
    filter: vehicleFilter.value,
    total: availableVehicles.value.length,
    filtered: filteredVehicles.value.length,
    vehicles: filteredVehicles.value.map(v => ({
      id: v.id,
      plateNumber: v.plateNumber,
      status: v.status,
      hasTask: v.hasTask,
      hasRunningTask: v.hasRunningTask
    }))
  })
}

// 查看车辆详情
const viewVehicleDetail = (vehicleId: string) => {
  router.push(`/vehicles/${vehicleId}`)
}

// 编辑任务
const editTask = (task: DispatchTask) => {
  router.push(`/tasks/${task.id}/edit`)
}

// 重新发送任务
const resendTask = async (task: DispatchTask) => {
  try {
    await ElMessageBox.confirm(
      `确定要重新发送任务 "${task.taskName}" 吗？将创建一个新的任务副本。`,
      '确认重新发送',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const { resendTaskApi } = await import('@/api/tasks')
    const response = await resendTaskApi(task.id)
    
    if (response.data.code === 200) {
      ElMessage.success({
        message: `任务重新发送成功，新任务编号: ${response.data.data.taskNo}`,
        duration: 5000
      })
      await loadData()
    } else {
      ElMessage.error(response.data.message || '重新发送失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '重新发送失败')
    }
  }
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
// 手动分配任务
const manualAssign = (task?: DispatchTask) => {
  // 根据任务类型加载不同的列表
  if (task?.taskType === '维护调度') {
    loadMaintenance()
  } else {
    loadDrivers()
  }
  
  assignForm.value = {
    taskId: task ? task.id.toString() : '',
    taskName: task ? task.taskName : '',
    vehicleId: '',
    driverUsername: '',
    maintenanceUsername: '',
    estimatedDuration: task?.estimatedDuration || 60,
    notes: ''
  }
  assignDialogVisible.value = true
}

// 任务选择变化
const handleTaskChange = (taskId: number) => {
  const task = pendingTasks.value.find(t => t.id === taskId)
  if (task) {
    assignForm.value.taskName = task.taskName
    assignForm.value.estimatedDuration = task.estimatedDuration || 60
    
    // 根据任务类型加载不同的列表
    if (task.taskType === '维护调度') {
      loadMaintenance()
      // 清空司机选择
      assignForm.value.driverUsername = ''
    } else {
      loadDrivers()
      // 清空维修员选择
      assignForm.value.maintenanceUsername = ''
    }
  }
}

// 分配任务给车辆
const assignTaskToVehicle = async (vehicle: Vehicle) => {
  if (pendingTasks.value.length === 0) {
    ElMessage.warning('没有待分配的任务')
    return
  }
  
  // 打开分配对话框，让用户选择任务
  manualAssign()
  assignForm.value.vehicleId = vehicle.id.toString()
}

// 维修申请
const requestMaintenance = (vehicle: Vehicle) => {
  ElMessage.info(`为车辆 ${vehicle.plateNumber} 提交维修申请功能开发中`)
}

// 取消分配任务
const unassignTask = async (task: DispatchTask) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消分配任务 "${task.taskName}" 吗？任务将恢复为待分配状态，司机将收到取消通知邮件。`,
      '确认取消分配',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const { unassignTaskApi } = await import('@/api/tasks')
    const response = await unassignTaskApi(task.id)
    
    if (response.data.code === 200) {
      // 乐观更新：立即更新本地状态
      const taskIndex = pendingTasks.value.findIndex(t => t.id === task.id)
      if (taskIndex === -1) {
        // 如果任务不在待分配列表中，添加它（因为取消分配后状态变为待分配）
        pendingTasks.value.push({
          ...task,
          status: 1,
          assignedVehicleId: null,
          assignedDriverId: null
        } as DispatchTask)
      }
      
      // 立即更新车辆状态（如果该车辆有任务）
      const vehicleIndex = availableVehicles.value.findIndex(v => v.id === task.assignedVehicleId?.toString())
      if (vehicleIndex !== -1) {
        const vehicle = availableVehicles.value[vehicleIndex]
        vehicle.hasTask = false
        vehicle.hasRunningTask = false
        vehicle.currentTask = undefined
      }
      
      ElMessage.success(response.data.message || '取消分配成功，邮件通知已发送')
      
      // 后台刷新完整数据（不阻塞UI）
      loading.value = true
      Promise.all([
        loadPendingTasks(),
        loadAvailableVehicles(),
        loadDispatchStats()
      ]).then(() => {
        filterVehicles()
      }).catch((error) => {
        console.error('后台刷新数据失败:', error)
      }).finally(() => {
        loading.value = false
      })
    } else {
      ElMessage.error(response.data.message || '取消分配失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '取消分配失败')
    }
  }
}

// 重置分配表单
const resetAssignForm = () => {
  assignForm.value = {
    taskId: '',
    taskName: '',
    vehicleId: '',
    driverUsername: '',
    estimatedDuration: 0,
    notes: ''
  }
  // 重置表单验证状态
  if (assignFormRef.value) {
    assignFormRef.value.resetFields()
  }
}

// 加载司机列表
const loadDrivers = async () => {
  if (driversLoaded.value && availableDrivers.value.length > 0) return
  
  try {
    const { getUsersApi } = await import('@/api/users')
    const response = await getUsersApi({ page: 0, size: 1000 })
    
    if (response.data.code === 200) {
      // 处理分页数据或直接数组数据
      let users: any[] = []
      if (response.data.data?.content && Array.isArray(response.data.data.content)) {
        users = response.data.data.content
      } else if (Array.isArray(response.data.data)) {
        users = response.data.data
      }
      
      // 筛选出司机角色的用户
      availableDrivers.value = users.filter((user: any) => {
        // 只显示启用的用户
        if (user.status !== 1 && user.status !== 'active') {
          return false
        }
        
        // 检查用户角色
        if (user.roles && Array.isArray(user.roles)) {
          // 如果 roles 是对象数组（包含 roleCode 或 roleName）
          if (user.roles.length > 0 && typeof user.roles[0] === 'object') {
            return user.roles.some((role: any) => 
              role.roleCode === 'DRIVER' || 
              role.roleName?.includes('司机') ||
              role.roleName?.toLowerCase().includes('driver')
            )
          } 
          // 如果 roles 是字符串数组（roleCode列表）
          else if (typeof user.roles[0] === 'string') {
            return user.roles.includes('DRIVER') || 
                   user.roles.some((code: string) => code.toLowerCase().includes('driver'))
          }
        }
        
        // 检查是否有role字段（单个角色）
        if (user.role === 'DRIVER' || user.role?.toLowerCase() === 'driver') {
          return true
        }
        
        return false
      })
      
      driversLoaded.value = true
      console.log('已加载司机列表:', availableDrivers.value.length, availableDrivers.value)
    }
  } catch (error) {
    console.error('加载司机列表失败:', error)
    ElMessage.warning('加载司机列表失败，请刷新重试')
  }
}

// 加载维修员列表
const loadMaintenance = async () => {
  if (maintenanceLoaded.value && availableMaintenance.value.length > 0) return
  
  try {
    const { getUsersApi } = await import('@/api/users')
    const response = await getUsersApi({ page: 0, size: 1000 })
    
    if (response.data.code === 200) {
      // 处理分页数据或直接数组数据
      let users: any[] = []
      if (response.data.data?.content && Array.isArray(response.data.data.content)) {
        users = response.data.data.content
      } else if (Array.isArray(response.data.data)) {
        users = response.data.data
      }
      
      // 筛选出维修员角色的用户
      availableMaintenance.value = users.filter((user: any) => {
        // 只显示启用的用户
        if (user.status !== 1 && user.status !== 'active') {
          return false
        }
        
        // 检查用户角色
        if (user.roles && Array.isArray(user.roles)) {
          // 如果 roles 是对象数组（包含 roleCode 或 roleName）
          if (user.roles.length > 0 && typeof user.roles[0] === 'object') {
            return user.roles.some((role: any) => 
              role.roleCode === 'MAINTENANCE' || 
              role.roleName?.includes('维修') ||
              role.roleName?.toLowerCase().includes('maintenance')
            )
          } 
          // 如果 roles 是字符串数组（roleCode列表）
          else if (typeof user.roles[0] === 'string') {
            return user.roles.includes('MAINTENANCE') || 
                   user.roles.some((code: string) => code.toLowerCase().includes('maintenance'))
          }
        }
        
        // 检查是否有role字段（单个角色）
        if (user.role === 'MAINTENANCE' || user.role?.toLowerCase() === 'maintenance') {
          return true
        }
        
        return false
      })
      
      maintenanceLoaded.value = true
      console.log('已加载维修员列表:', availableMaintenance.value.length, availableMaintenance.value)
    }
  } catch (error) {
    console.error('加载维修员列表失败:', error)
    ElMessage.warning('加载维修员列表失败，请刷新重试')
  }
}

// 确认分配
const confirmAssign = async () => {
  if (!assignFormRef.value) return
  
  try {
    await assignFormRef.value.validate()
    
    const task = pendingTasks.value.find(t => t.id === Number(assignForm.value.taskId))
    const isMaintenanceTask = task?.taskType === '维护调度'
    
    // 根据任务类型验证不同的字段
    if (isMaintenanceTask) {
      if (!assignForm.value.maintenanceUsername) {
        ElMessage.warning('请选择维修员')
        return
      }
    } else {
      if (!assignForm.value.driverUsername) {
        ElMessage.warning('请选择司机')
        return
      }
    }
    
    let response: any
    
    if (isMaintenanceTask) {
      // 调用维修任务分配API
      const { assignTaskToMaintenanceApi } = await import('@/api/tasks')
      response = await assignTaskToMaintenanceApi(
        Number(assignForm.value.taskId),
        Number(assignForm.value.vehicleId),
        undefined,
        assignForm.value.maintenanceUsername
      )
    } else {
      // 调用普通任务分配API，传递司机用户名
      const { assignTaskApi } = await import('@/api/tasks')
      response = await assignTaskApi(
        Number(assignForm.value.taskId),
        Number(assignForm.value.vehicleId),
        undefined,
        assignForm.value.driverUsername
      )
    }
    
    if (response.data.code === 200) {
      const updatedTask = response.data.data
      
      // 乐观更新：立即更新本地状态
      // 从待分配任务列表中移除该任务
      const taskIndex = pendingTasks.value.findIndex(t => t.id === updatedTask.id)
      if (taskIndex !== -1) {
        pendingTasks.value.splice(taskIndex, 1)
      }
      
      // 立即更新车辆状态
      const vehicleIndex = availableVehicles.value.findIndex(v => v.id === assignForm.value.vehicleId)
      if (vehicleIndex !== -1) {
        const vehicle = availableVehicles.value[vehicleIndex]
        vehicle.hasTask = true
        vehicle.hasRunningTask = false
        vehicle.currentTask = updatedTask as DispatchTask
      }
      
      ElMessage.success(response.data.message || '任务分配成功，邮件通知已发送')
      assignDialogVisible.value = false
      
      // 立即更新筛选后的车辆列表
      filterVehicles()
      
      // 后台刷新完整数据（不阻塞UI）
      loading.value = true
      Promise.all([
        loadPendingTasks(),
        loadAvailableVehicles(),
        loadDispatchStats()
      ]).then(() => {
        filterVehicles()
      }).catch((error) => {
        console.error('后台刷新数据失败:', error)
      }).finally(() => {
        loading.value = false
      })
    } else {
      ElMessage.error(response.data.message || '分配失败')
    }
  } catch (error: any) {
    if (error?.message !== 'cancel' && !error?.response) {
      // 表单验证失败
      return
    }
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '分配失败')
    }
  }
}

// 刷新数据
const refreshData = async () => {
  await loadData()
  await loadInProgressTasks()
  ElMessage.success('数据已刷新')
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 先加载任务数据，再加载车辆数据（车辆数据需要任务数据来标记hasTask）
    await Promise.all([
      loadDispatchStats(),
      loadPendingTasks(),
      loadInProgressTasks()
    ])
    // 车辆数据加载会使用任务数据，所以单独加载
    await loadAvailableVehicles()
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 加载调度统计数据
const loadDispatchStats = async () => {
  try {
    const { getDispatchStatsApi } = await import('@/api/statistics')
    const response = await getDispatchStatsApi()
    
    if (response.data.code === 200) {
      const data = response.data.data
      dispatchStats.value = {
        pendingTasks: data.pendingTasks || 0,
        availableVehicles: data.availableVehicles || 0,
        idleRate: data.idleRate || 0,
        dispatchEfficiency: data.dispatchEfficiency || 0,
        efficiencyChange: data.efficiencyChange || 0,
        avgResponseTime: data.avgResponseTime || 0
      }
    }
  } catch (error) {
    console.error('Load dispatch stats failed:', error)
    ElMessage.error('加载调度统计数据失败')
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
    const { getVehiclesApi } = await import('@/api/vehicles')
    const { getTasksApi } = await import('@/api/tasks')
    
    // 同时加载车辆和任务数据
    // 注意：使用getVehiclesApi获取所有车辆，而不是只获取正常状态的车辆
    const [vehiclesResponse, tasksResponse] = await Promise.all([
      getVehiclesApi(),  // 获取所有车辆，包括维修中、故障等状态
      getTasksApi()
    ])
    
    // 处理车辆数据（支持多种响应格式）
    let vehicles: any[] = []
    if (vehiclesResponse.data.code === 200) {
      if (Array.isArray(vehiclesResponse.data.data)) {
        vehicles = vehiclesResponse.data.data
      } else if (vehiclesResponse.data.data?.content) {
        vehicles = vehiclesResponse.data.data.content
      }
    }
    
    // 处理任务数据
    const allTasks: DispatchTask[] = tasksResponse.data?.code === 200 && Array.isArray(tasksResponse.data.data) 
      ? tasksResponse.data.data 
      : (tasksResponse.data?.data?.content || [])
    
    // 为每个车辆查找关联的任务
    availableVehicles.value = vehicles.map(v => {
        // 查找该车辆的任务（状态为2-已分配或3-执行中）
        // 注意：确保ID类型匹配（都转换为number进行比较）
        const vehicleTasks = allTasks.filter((t: DispatchTask) => {
          if (!t.assignedVehicleId) return false
          
          const taskVehicleId = typeof t.assignedVehicleId === 'string' 
            ? Number(t.assignedVehicleId) 
            : t.assignedVehicleId
          const vehicleId = typeof v.id === 'string' ? Number(v.id) : v.id
          
          const matches = taskVehicleId === vehicleId && (t.status === 2 || t.status === 3)
          
          if (matches) {
            console.log('找到车辆任务匹配:', {
              vehicleId: v.id,
              vehicleNo: v.vehicleNo,
              taskId: t.id,
              taskName: t.taskName,
              taskStatus: t.status,
              assignedVehicleId: t.assignedVehicleId
            })
          }
          
          return matches
        })
        const hasRunningTask = vehicleTasks.some((t: DispatchTask) => t.status === 3)
        // 获取当前任务（优先显示执行中的任务，否则显示已分配的任务）
        const currentTask = vehicleTasks.find((t: DispatchTask) => t.status === 3) 
          || vehicleTasks.find((t: DispatchTask) => t.status === 2)
          || null
        
        const vehicleData = {
          ...v,
          id: v.id.toString(),
          plateNumber: v.vehicleNo,
          vehicleType: `类型${v.vehicleTypeId}`,
          status: v.status,
          location: v.locationAddress || '未知',
          distanceToTasks: 0,
          currentLoad: 0,
          efficiencyScore: 90,
          hasTask: vehicleTasks.length > 0,
          hasRunningTask: hasRunningTask,
          currentTask: currentTask || undefined
        } as VehicleWithTask
        
        if (vehicleData.hasTask) {
          console.log('车辆有任务:', {
            vehicleId: vehicleData.id,
            plateNumber: vehicleData.plateNumber,
            status: vehicleData.status,
            hasTask: vehicleData.hasTask,
            hasRunningTask: vehicleData.hasRunningTask,
            tasks: vehicleTasks.map(t => ({ id: t.id, name: t.taskName, status: t.status }))
          })
        }
        
        return vehicleData
      })
      
      console.log('加载车辆数据完成:', {
        totalVehicles: availableVehicles.value.length,
        vehiclesWithTasks: availableVehicles.value.filter(v => v.hasTask).length,
        allTasks: allTasks.length,
        tasksWithVehicles: allTasks.filter(t => t.assignedVehicleId).map(t => ({
          taskId: t.id,
          taskName: t.taskName,
          assignedVehicleId: t.assignedVehicleId,
          status: t.status
        }))
      })
      
      // 应用当前筛选（确保筛选逻辑正确执行）
      filterVehicles()
  } catch (error) {
    console.error('Load available vehicles failed:', error)
    availableVehicles.value = []
    filteredVehicles.value = []
  }
}

// 检查是否可以完成任务
const canCompleteTask = (task: DispatchTask): boolean => {
  // 检查任务状态
  if (task.status !== 2 && task.status !== 3) {
    return false
  }
  // 检查权限
  const hasPerm = hasPermission('task:complete')
  // 调试日志（仅在开发环境）
  if (!hasPerm && import.meta.env.DEV) {
    console.log('用户没有task:complete权限', {
      userInfo: userStore.userInfo,
      permissions: userStore.userInfo?.permissions
    })
  }
  return hasPerm
}

// 完成任务
const completeTask = async (task: DispatchTask) => {
  try {
    await ElMessageBox.confirm(
      `确定要完成任务 "${task.taskName}" 吗？`,
      '确认完成任务',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const { completeTaskApi } = await import('@/api/tasks')
    const response = await completeTaskApi(task.id)
    
    if (response.data.code === 200) {
      ElMessage.success('任务已完成')
      // 立即刷新数据
      await loadData()
      await loadAvailableVehicles()
    } else {
      ElMessage.error(response.data.message || '完成任务失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '完成任务失败')
    }
  }
}

// 查看任务详情
const viewTaskDetail = (task: DispatchTask) => {
  router.push(`/tasks/${task.id}`)
}

// 获取车辆号
const getVehicleNo = (vehicleId: any) => {
  if (!vehicleId) return '-'
  const vehicle = availableVehicles.value.find(v => {
    const vId = typeof v.id === 'string' ? Number(v.id) : v.id
    const tVId = typeof vehicleId === 'string' ? Number(vehicleId) : vehicleId
    return vId === tVId
  })
  return vehicle?.plateNumber || vehicleId
}

// 加载进行中任务
const loadInProgressTasks = async () => {
  try {
    const { getTasksApi } = await import('@/api/tasks')
    const response = await getTasksApi({ status: undefined }) // 获取所有任务
    
    if (response.data.code === 200) {
      let allTasks: DispatchTask[] = []
      if (Array.isArray(response.data.data)) {
        allTasks = response.data.data
      } else if (response.data.data?.content) {
        allTasks = response.data.data.content
      }
      
      // 筛选出状态为2（已分配）或3（执行中）的任务
      inProgressTasks.value = allTasks.filter((task: DispatchTask) => 
        task.status === 2 || task.status === 3
      )
    }
  } catch (error) {
    console.error('Load in-progress tasks failed:', error)
    inProgressTasks.value = []
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
  loadInProgressTasks()
  
  // 设置自动刷新（每30秒刷新一次）
  refreshTimer = setInterval(() => {
    loadData()
    loadInProgressTasks()
  }, 30000)
})

// 组件卸载时清除定时器
onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
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