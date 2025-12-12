<template>
  <div class="dashboard-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">仪表盘</h1>
      <p class="page-description">机场车辆监控与调度系统总览</p>
    </div>
    
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
        <div class="stat-icon">
          <el-icon><Van /></el-icon>
        </div>
        <div class="stat-value">{{ vehicleStats.totalCount }}</div>
        <div class="stat-label">车辆总数</div>
        <div class="stat-trend">
          <span v-if="vehicleStats.activeCount > 0" class="trend-up">
            ↑ {{ vehicleStats.activeCount }} 正常运行
          </span>
        </div>
      </div>
      
      <div class="stat-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
        <div class="stat-icon">
          <el-icon><List /></el-icon>
        </div>
        <div class="stat-value">{{ taskStats.totalCount }}</div>
        <div class="stat-label">任务总数</div>
        <div class="stat-trend">
          <span class="trend-today">今日 {{ taskStats.todayCount }} 个</span>
        </div>
      </div>
      
      <div class="stat-card" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
        <div class="stat-icon">
          <el-icon><Odometer /></el-icon>
        </div>
        <div class="stat-value">{{ taskStats.inProgressCount }}</div>
        <div class="stat-label">进行中任务</div>
        <div class="stat-trend">
          <span class="trend-active">
            {{ taskStats.pendingCount }} 待分配
          </span>
        </div>
      </div>
      
      <div class="stat-card" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
        <div class="stat-icon">
          <el-icon><Check /></el-icon>
        </div>
        <div class="stat-value">{{ taskStats.completedCount }}</div>
        <div class="stat-label">已完成任务</div>
        <div class="stat-trend">
          <span class="trend-completed">
            成功率 {{ completionRate }}%
          </span>
        </div>
      </div>
    </div>
    
    <!-- 内容区域 -->
    <div class="dashboard-content">
      <!-- 最近任务 -->
      <div class="dashboard-section">
        <div class="section-header">
          <h3 class="section-title">最近任务</h3>
          <el-button type="primary" link @click="$router.push('/tasks')">
            查看全部
          </el-button>
        </div>
        <div class="task-list">
          <div v-if="recentTasks.length === 0" class="empty-state">
            <el-empty description="暂无任务数据" />
          </div>
          <div v-else class="task-items">
            <div
              v-for="task in recentTasks"
              :key="task.id"
              class="task-item"
              @click="$router.push(`/tasks/${task.id}`)"
            >
              <div class="task-info">
                <div class="task-name">{{ task.taskName }}</div>
                <div class="task-location">
                  {{ task.startLocation }} → {{ task.endLocation }}
                </div>
                <div class="task-time">
                  {{ formatTime(task.startTime) }}
                </div>
              </div>
              <div class="task-status">
                <el-tag :type="getTaskStatusType(task.status)" size="small">
                  {{ getTaskStatusText(task.status) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 车辆状态 -->
      <div class="dashboard-section">
        <div class="section-header">
          <h3 class="section-title">车辆状态</h3>
          <el-button type="primary" link @click="$router.push('/vehicles')">
            管理车辆
          </el-button>
        </div>
        <div class="vehicle-stats">
          <div class="vehicle-stat-item">
            <div class="stat-number">{{ vehicleStats.activeCount }}</div>
            <div class="stat-label">正常运行</div>
          </div>
          <div class="vehicle-stat-item">
            <div class="stat-number">{{ vehicleStats.maintenanceCount }}</div>
            <div class="stat-label">维修中</div>
          </div>
          <div class="vehicle-stat-item">
            <div class="stat-number">{{ vehicleStats.faultCount }}</div>
            <div class="stat-label">故障</div>
          </div>
          <div class="vehicle-stat-item">
            <div class="stat-number">{{ vehicleStats.offlineCount }}</div>
            <div class="stat-label">离线</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { 
  Van, List, Odometer, Check 
} from '@element-plus/icons-vue'
import { getVehicleStatisticsApi } from '@/api/vehicles'
import { getTaskStatisticsApi, getTasksApi } from '@/api/tasks'
import type { VehicleStatistics, TaskStatistics, DispatchTask } from '@/api/types'
import dayjs from 'dayjs'

// 数据状态
const vehicleStats = ref<VehicleStatistics>({
  totalCount: 0,
  activeCount: 0,
  maintenanceCount: 0,
  faultCount: 0,
  offlineCount: 0
})

const taskStats = ref<TaskStatistics>({
  totalCount: 0,
  pendingCount: 0,
  assignedCount: 0,
  inProgressCount: 0,
  completedCount: 0,
  cancelledCount: 0,
  exceptionCount: 0,
  todayCount: 0
})

const recentTasks = ref<DispatchTask[]>([])

// 计算属性
const completionRate = computed(() => {
  if (taskStats.value.totalCount === 0) return 0
  return Math.round((taskStats.value.completedCount / taskStats.value.totalCount) * 100)
})

// 获取任务状态类型
const getTaskStatusType = (status: number) => {
  const statusMap: Record<number, string> = {
    1: 'info',     // 待分配
    2: 'warning',  // 已分配
    3: 'primary',  // 执行中
    4: 'success',  // 已完成
    5: 'danger',   // 已取消
    6: 'danger'    // 异常
  }
  return statusMap[status] || 'info'
}

// 获取任务状态文本
const getTaskStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '待分配',
    2: '已分配',
    3: '执行中',
    4: '已完成',
    5: '已取消',
    6: '异常'
  }
  return statusMap[status] || '未知'
}

// 格式化时间
const formatTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm')
}

// 加载仪表盘数据
const loadDashboardData = async () => {
  try {
    // 并行加载统计数据
    const [vehicleRes, taskRes] = await Promise.all([
      getVehicleStatisticsApi(),
      getTaskStatisticsApi()
    ])
    
    if (vehicleRes.data.code === 200) {
      vehicleStats.value = vehicleRes.data.data
    }
    
    if (taskRes.data.code === 200) {
      taskStats.value = taskRes.data.data
    }
    
    // 加载最近任务
    const tasksRes = await getTasksApi()
    if (tasksRes.data.code === 200) {
      const allTasks = Array.isArray(tasksRes.data.data) ? tasksRes.data.data : []
      recentTasks.value = allTasks.slice(0, 5)
    }
  } catch (error) {
    console.error('Load dashboard data failed:', error)
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  .page-header {
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
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  }
  
  .stat-icon {
    font-size: 36px;
    color: rgba(255, 255, 255, 0.9);
    margin-bottom: 16px;
  }
  
  .stat-value {
    font-size: 32px;
    font-weight: 700;
    color: white;
    margin-bottom: 8px;
  }
  
  .stat-label {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.8);
    margin-bottom: 12px;
  }
  
  .stat-trend {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.7);
    
    .trend-up {
      color: rgba(255, 255, 255, 0.9);
    }
  }
}

.dashboard-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.dashboard-section {
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
    }
  }
}

.task-list {
  .empty-state {
    padding: 40px 20px;
    text-align: center;
  }
  
  .task-items {
    .task-item {
      padding: 16px 20px;
      border-bottom: 1px solid var(--border-lighter-color);
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      transition: background-color 0.3s ease;
      
      &:hover {
        background-color: var(--background-extra-light-color);
      }
      
      &:last-child {
        border-bottom: none;
      }
      
      .task-info {
        flex: 1;
        
        .task-name {
          font-size: 16px;
          font-weight: 600;
          color: var(--text-primary-color);
          margin-bottom: 4px;
        }
        
        .task-location {
          font-size: 14px;
          color: var(--text-regular-color);
          margin-bottom: 4px;
        }
        
        .task-time {
          font-size: 12px;
          color: var(--text-secondary-color);
        }
      }
      
      .task-status {
        margin-left: 16px;
      }
    }
  }
}

.vehicle-stats {
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  
  .vehicle-stat-item {
    text-align: center;
    padding: 20px;
    background: var(--background-extra-light-color);
    border-radius: 8px;
    
    .stat-number {
      font-size: 24px;
      font-weight: 700;
      color: var(--primary-color);
      margin-bottom: 8px;
    }
    
    .stat-label {
      font-size: 14px;
      color: var(--text-regular-color);
    }
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .dashboard-content {
    grid-template-columns: 1fr;
  }
  
  .vehicle-stats {
    grid-template-columns: 1fr;
  }
}
</style>